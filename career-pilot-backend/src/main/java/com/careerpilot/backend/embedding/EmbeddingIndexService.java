package com.careerpilot.backend.embedding;

import com.careerpilot.backend.entity.QuestionBank;
import com.careerpilot.backend.entity.Track;
import com.careerpilot.backend.repository.IQuestionBankRepository;
import com.careerpilot.backend.repository.ITrackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Single entry-point for all vector-store operations.
 *
 * <p>
 * Handles indexing, removal, and similarity search for
 * {@link Track} and {@link QuestionBank} entities.
 *
 * <p>
 * The full reindex ({@link #reindexAll()}) is async and self-skipping:
 * it only runs when the vector store is empty, so it is safe to call on
 * every startup without re-embedding on each boot.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingIndexService {

  private static final String NOT_SET = "not-set";

  private final VectorStore vectorStore;
  private final VectorState vectorState;
  private final ITrackRepository trackRepository;
  private final IQuestionBankRepository questionRepository;

  @Value("${spring.ai.openai.embedding.api-key:}")
  private String embeddingApiKey;

  @Value("${spring.ai.openai.embedding.track-match-threshold:0.45}")
  private double trackMatchThreshold;

  /**
   * Asynchronously rebuilds the entire vector index.
   * No-op when the index is already complete.
   *
   * <p>Resumable by design: rows are upserted by stable id and stale docs removed,
   * so an interrupted run is simply continued on the next boot instead of leaving
   * the store partially wiped.</p>
   */
  @Async
  public void reindexAll() {
    if (!embeddingConfigured()) {
      log.warn("Embedding API key not configured – skipping reindex");
      return;
    }
    if (isIndexPopulated()) {
      log.info("Vector store already complete – skipping reindex");
      return;
    }
    if (!vectorState.tryAcquireReindexLock()) {
      log.info("Reindex already running on another instance – skipping");
      return;
    }
    try {
      reindexTracks();
      reindexQuestions();
    } finally {
      vectorState.releaseReindexLock();
    }
  }

  public void indexTrack(Track track) {
    vectorStore.add(List.of(trackDocument(track)));
  }

  public void removeTrack(Long id) {
    vectorStore.delete(List.of(stableId(VectorState.OBJECT_TYPE_TRACK, id)));
  }

  public Optional<Track> matchTrack(String jobText, String fallbackTitle) {
    if (vectorState.countByType(VectorState.OBJECT_TYPE_TRACK) == 0) {
      return matchTrackLexical(fallbackTitle, jobText);
    }

    List<Document> results = vectorStore.similaritySearch(
        SearchRequest.builder()
            .query(jobText)
            .topK(1)
            .similarityThreshold(trackMatchThreshold)
            .filterExpression(new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key("objectType"),
                new Filter.Value(VectorState.OBJECT_TYPE_TRACK)))
            .build());

    if (results == null || results.isEmpty()) {
      log.warn("No embedding match for job '{}' – falling back to lexical track match", fallbackTitle);
      return matchTrackLexical(fallbackTitle, jobText);
    }

    Object rawId = results.get(0).getMetadata().get("objectId");
    if (!(rawId instanceof Number number)) {
      log.warn("Unexpected objectId metadata type: {}", rawId);
      return matchTrackLexical(fallbackTitle, jobText);
    }

    return trackRepository.findById(number.longValue());
  }

  public void indexQuestion(QuestionBank question) {
    vectorStore.add(List.of(questionDocument(question)));
  }

  public void removeQuestion(Long id) {
    vectorStore.delete(List.of(stableId(VectorState.OBJECT_TYPE_QUESTION, id)));
  }

  private void reindexTracks() {
    List<Track> tracks = trackRepository.findByIsActiveTrue();
    vectorStore.add(tracks.stream().map(this::trackDocument).toList());
    long removed = vectorState.removeStale(VectorState.OBJECT_TYPE_TRACK, "tracks");
    log.info("Reindexed {} active tracks (stale removed: {})", tracks.size(), removed);
  }

  private void reindexQuestions() {
    List<QuestionBank> questions = questionRepository.findAllActiveWithTrack();
    vectorStore.add(questions.stream().map(this::questionDocument).toList());
    long removed = vectorState.removeStale(VectorState.OBJECT_TYPE_QUESTION, "question_bank");
    log.info("Reindexed {} active questions (stale removed: {})", questions.size(), removed);
  }

  private Document trackDocument(Track track) {
    String text = (track.getDescription() == null || track.getDescription().isBlank())
        ? track.getName()
        : track.getName() + "\n" + track.getDescription();

    return Document.builder()
        .id(stableId(VectorState.OBJECT_TYPE_TRACK, track.getId()))
        .text(text)
        .metadata(Map.of(
            "objectType", VectorState.OBJECT_TYPE_TRACK,
            "objectId", track.getId(),
            "trackName", track.getName()))
        .build();
  }

  private Document questionDocument(QuestionBank question) {
    StringBuilder text = new StringBuilder(question.getQuestionText());
    if (question.getExpectedKeywords() != null && !question.getExpectedKeywords().isBlank()) {
      text.append("\n").append(question.getExpectedKeywords());
    }
    if (question.getCategory() != null) {
      text.append("\n").append(question.getCategory());
    }

    return Document.builder()
        .id(stableId(VectorState.OBJECT_TYPE_QUESTION, question.getId()))
        .text(text.toString())
        .metadata(Map.of(
            "objectType", VectorState.OBJECT_TYPE_QUESTION,
            "objectId", question.getId(),
            "trackName", question.getTrack().getName()))
        .build();
  }

  private String stableId(String objectType, Long id) {
    return UUID.nameUUIDFromBytes(
        (objectType + "-" + id).getBytes(StandardCharsets.UTF_8)).toString();
  }

  private boolean isIndexPopulated() {
      long dbTracks    = trackRepository.countByIsActiveTrue();
      long dbQuestions = questionRepository.countActiveWithTrack();

      long indexedTracks    = vectorState.countByType(VectorState.OBJECT_TYPE_TRACK);
      long indexedQuestions = vectorState.countByType(VectorState.OBJECT_TYPE_QUESTION);

      // Complete only when counts match exactly: covers missing rows AND stale/duplicate docs.
      boolean tracksOk    = indexedTracks    == dbTracks;
      boolean questionsOk = indexedQuestions == dbQuestions;

      if (!tracksOk || !questionsOk) {
          log.info("Vector store incomplete – tracks: {}/{}, questions: {}/{}",
                  indexedTracks, dbTracks, indexedQuestions, dbQuestions);
      }
      return tracksOk && questionsOk;
  }

  private boolean embeddingConfigured() {
    return embeddingApiKey != null
        && !embeddingApiKey.isBlank()
        && !NOT_SET.equals(embeddingApiKey);
  }

  private Optional<Track> matchTrackLexical(String jobTitle, String jobContext) {
    if (jobTitle == null || jobTitle.isBlank()) {
      return Optional.empty();
    }
    String normalizedTitle = normalize(jobTitle);
    if (normalizedTitle.isBlank()) {
      return Optional.empty();
    }

    List<Track> tracks = trackRepository.findByIsActiveTrue();
    if (tracks.isEmpty()) {
      return Optional.empty();
    }

    List<String> titleTokens = tokenize(normalizedTitle);
    // The job description / job text adds context, but stays secondary to the title.
    List<String> contextTokens = jobContext != null && !jobContext.isBlank()
        ? tokenize(normalize(jobContext))
        : List.of();

    // Inverse-document-frequency weighting: a token is more discriminating
    // when it appears in fewer tracks (e.g. "android" > "engineer").
    Map<String, Long> tokenDocFreq = new HashMap<>();
    for (Track track : tracks) {
      Set<String> toks = new HashSet<>(tokenize(normalize(track.getName())));
      toks.addAll(tokenize(normalize(track.getDescription())));
      for (String tok : toks) {
        tokenDocFreq.merge(tok, 1L, Long::sum);
      }
    }

    Track best = null;
    double bestScore = 0;
    for (Track track : tracks) {
      String nameNorm = normalize(track.getName());
      if (nameNorm.isBlank()) {
        continue;
      }
      String descNorm = normalize(track.getDescription());
      Set<String> nameTokens = new HashSet<>(tokenize(nameNorm));
      Set<String> descTokens = new HashSet<>(tokenize(descNorm));

      double score = 0;
      // Exact phrase containment of the track name in the job title wins immediately.
      if (normalizedTitle.contains(nameNorm)) {
        score += 100;
      }
      for (String tok : titleTokens) {
        score += overlapWeight(tok, nameTokens, descTokens, tokenDocFreq);
      }
      // Description-backed context contributes less so it can't hijack a title match.
      for (String tok : contextTokens) {
        if (nameTokens.contains(tok)) {
          score += overlapWeight(tok, nameTokens, descTokens, tokenDocFreq);
        } else if (descTokens.contains(tok)) {
          score += 0.5 * overlapWeight(tok, nameTokens, descTokens, tokenDocFreq);
        }
      }
      if (score > bestScore) {
        bestScore = score;
        best = track;
      }
    }

    if (bestScore <= 0) {
      log.warn("No lexical track match for job title '{}'", jobTitle);
      return Optional.empty();
    }
    return Optional.of(best);
  }

  private double overlapWeight(String token, Set<String> nameTokens, Set<String> descTokens,
                               Map<String, Long> tokenDocFreq) {
    long df = tokenDocFreq.getOrDefault(token, 0L);
    double weight = 1.0 / (1 + Math.log(df == 0 ? 1 : df));
    if (nameTokens.contains(token)) {
      return 4 * weight;
    }
    return descTokens.contains(token) ? 1.5 * weight : 0;
  }

  private String normalize(String text) {
    if (text == null) {
      return "";
    }
    return text.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim();
  }

  private List<String> tokenize(String text) {
    if (text == null || text.isBlank()) {
      return List.of();
    }
    return Arrays.stream(text.split("\\s+"))
        .filter(t -> !t.isBlank())
        .toList();
  }
}
