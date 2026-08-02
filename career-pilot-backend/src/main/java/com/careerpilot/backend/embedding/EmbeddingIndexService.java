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
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
   * No-op when the index is already populated.
   */
  @Async
  public void reindexAll() {
    if (!embeddingConfigured()) {
      log.warn("Embedding API key not configured – skipping reindex");
      return;
    }
    if (isIndexPopulated()) {
      log.info("Vector store already populated – skipping reindex");
      return;
    }
    reindexTracks();
    reindexQuestions();
  }

  public void indexTrack(Track track) {
    vectorStore.add(List.of(trackDocument(track)));
  }

  public void removeTrack(Long id) {
    vectorStore.delete(List.of(stableId(VectorState.OBJECT_TYPE_TRACK, id)));
  }

  public Optional<Track> matchTrack(String jobText, String fallbackTitle) {
    if (vectorState.countByType(VectorState.OBJECT_TYPE_TRACK) == 0) {
      return matchTrackByTitle(fallbackTitle);
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
      return Optional.empty();
    }

    Object rawId = results.get(0).getMetadata().get("objectId");
    if (!(rawId instanceof Number number)) {
      log.warn("Unexpected objectId metadata type: {}", rawId);
      return Optional.empty();
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
    vectorStore.delete(new FilterExpressionBuilder()
        .eq("objectType", VectorState.OBJECT_TYPE_TRACK)
        .build());

    List<Track> tracks = trackRepository.findByIsActiveTrue();
    vectorStore.add(tracks.stream().map(this::trackDocument).toList());
    log.info("Reindexed {} active tracks", tracks.size());
  }

  private void reindexQuestions() {
    vectorStore.delete(new FilterExpressionBuilder()
        .eq("objectType", VectorState.OBJECT_TYPE_QUESTION)
        .build());

    List<QuestionBank> questions = questionRepository.findAllActiveWithTrack();
    vectorStore.add(questions.stream().map(this::questionDocument).toList());
    log.info("Reindexed {} active questions", questions.size());
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
    return vectorState.countByType(VectorState.OBJECT_TYPE_TRACK) > 0
        && vectorState.countByType(VectorState.OBJECT_TYPE_QUESTION) > 0;
  }

  private boolean embeddingConfigured() {
    return embeddingApiKey != null
        && !embeddingApiKey.isBlank()
        && !NOT_SET.equals(embeddingApiKey);
  }

  private Optional<Track> matchTrackByTitle(String jobTitle) {
    if (jobTitle == null || jobTitle.isBlank()) {
      return Optional.empty();
    }
    String title = jobTitle.toLowerCase();
    return trackRepository.findByIsActiveTrue().stream()
        .filter(t -> t.getName() != null && !t.getName().isBlank())
        .filter(t -> {
          String name = t.getName().toLowerCase();
          return title.contains(name) || name.contains(title);
        })
        .max(Comparator.comparingInt(t -> t.getName().length()));
  }
}
