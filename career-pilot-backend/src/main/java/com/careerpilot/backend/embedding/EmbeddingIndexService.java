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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
  @Value("${spring.ai.openai.embedding.question-match-threshold:0.45}")
  private double questionMatchThreshold;

  /**
   * Asynchronously rebuilds the entire vector index.
   * No-op when the index is already complete.
   *
   * <p>
   * Resumable by design: rows are upserted by stable id and stale docs removed,
   * so an interrupted run is simply continued on the next boot instead of leaving
   * the store partially wiped.
   * </p>
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
      return matchTrackLexical(fallbackTitle);
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
      return matchTrackLexical(fallbackTitle);
    }

    Object rawId = results.get(0).getMetadata().get("objectId");
    if (!(rawId instanceof Number number)) {
      log.warn("Unexpected objectId metadata type: {}", rawId);
      return matchTrackLexical(fallbackTitle);
    }

    return trackRepository.findById(number.longValue());
  }

  public void indexQuestion(QuestionBank question) {
    vectorStore.add(List.of(questionDocument(question)));
  }

  public void removeQuestion(Long id) {
    vectorStore.delete(List.of(stableId(VectorState.OBJECT_TYPE_QUESTION, id)));
  }

  public List<QuestionBank> matchQuestions(String query, int topK) {
    if (query == null || query.isBlank() || topK <= 0) {
      return List.of();
    }
    if (vectorState.countByType(VectorState.OBJECT_TYPE_QUESTION) == 0) {
      return matchQuestionLexical(query, topK);
    }

    List<Document> results = vectorStore.similaritySearch(
        SearchRequest.builder()
            .query(query)
            .topK(topK)
            .similarityThreshold(questionMatchThreshold)
            .filterExpression(new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key("objectType"),
                new Filter.Value(VectorState.OBJECT_TYPE_QUESTION)))
            .build());

    if (results == null || results.isEmpty()) {
      log.warn("No embedding match for question query '{}' – falling back to lexical question match", query);
      return matchQuestionLexical(query, topK);
    }

    List<Long> ids = results.stream()
        .map(doc -> doc.getMetadata().get("objectId"))
        .filter(Number.class::isInstance)
        .map(id -> ((Number) id).longValue())
        .toList();

    if (ids.isEmpty()) {
      return matchQuestionLexical(query, topK);
    }

    Map<Long, QuestionBank> byId = questionRepository.findAllById(ids).stream()
        .collect(Collectors.toMap(QuestionBank::getId, Function.identity()));

    return ids.stream()
        .map(byId::get)
        .filter(Objects::nonNull)
        .filter(q -> Boolean.TRUE.equals(q.getIsActive()))
        .toList();
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
    long dbTracks = trackRepository.countByIsActiveTrue();
    long dbQuestions = questionRepository.countActiveWithTrack();

    long indexedTracks = vectorState.countByType(VectorState.OBJECT_TYPE_TRACK);
    long indexedQuestions = vectorState.countByType(VectorState.OBJECT_TYPE_QUESTION);

    // Complete only when counts match exactly: covers missing rows AND
    // stale/duplicate docs.
    boolean tracksOk = indexedTracks == dbTracks;
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

  private Optional<Track> matchTrackLexical(String jobTitle) {
    if (jobTitle == null || jobTitle.isBlank()) {
      return Optional.empty();
    }

    Page<Long> page = trackRepository.searchRelevantIds(
        jobTitle.trim(),
        PageRequest.of(0, 1));

    List<Long> ids = page.getContent();
    if (ids.isEmpty()) {
      log.warn("No lexical track match for job title '{}'", jobTitle);
      return Optional.empty();
    }

    return trackRepository.findById(ids.get(0));
  }

  private List<QuestionBank> matchQuestionLexical(String query, int topK) {
    if (query == null || query.isBlank() || topK <= 0) {
      return List.of();
    }

    Page<Long> relevantQuestions = questionRepository.searchRelevantIds(
        query.trim(),
        PageRequest.of(0, topK));

    List<Long> ids = relevantQuestions.getContent();
    if (ids.isEmpty()) {
      return List.of();
    }

    Map<Long, QuestionBank> questionsById = questionRepository.findAllById(ids)
        .stream()
        .collect(Collectors.toMap(
            QuestionBank::getId,
            Function.identity()));

    return ids.stream()
        .map(questionsById::get)
        .filter(Objects::nonNull)
        .toList();
  }
}
