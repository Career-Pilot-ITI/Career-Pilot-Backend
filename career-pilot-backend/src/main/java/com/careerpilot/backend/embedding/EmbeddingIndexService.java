package com.careerpilot.backend.embedding;

import com.careerpilot.backend.entity.QuestionBank;
import com.careerpilot.backend.entity.Track;
import com.careerpilot.backend.repository.IQuestionBankRepository;
import com.careerpilot.backend.repository.ITrackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Indexes active Tracks and QuestionBank entries into the Spring AI {@code vector_store}
 * so later features can do semantic job→track matching and track→question retrieval.
 *
 * <p>The full index is a one-off, async, self-skipping operation: the table lives in the
 * outsourced Postgres DB, so once populated the sweep does not re-embed on later boots.
 * Individual create/update/delete operations re-embed only the affected row.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingIndexService {

    static final String OBJECT_TYPE_TRACK = "TRACK";
    static final String OBJECT_TYPE_QUESTION = "QUESTION";

    private static final String NOT_SET = "not-set";

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final ITrackRepository trackRepository;
    private final IQuestionBankRepository questionRepository;

    @Value("${spring.ai.openai.embedding.api-key:}")
    private String embeddingApiKey;

    /**
     * One-off background sweep: populates the index only when {@code vector_store} is empty.
     */
    @Async
    public void reindexAll() {
        if (!embeddingConfigured()) {
            log.warn("Embedding API key not configured - skipping embedding backfill");
            return;
        }
        if (isIndexPopulated()) {
            log.info("vector_store already populated with {} documents - skipping one-off backfill",
                    countDocuments());
            return;
        }
        reindexTracks();
        reindexQuestions();
    }

    public void indexTrack(Track track) {
        vectorStore.add(List.of(trackDocument(track)));
    }

    public void removeTrack(Long id) {
        vectorStore.delete(List.of(stableId(OBJECT_TYPE_TRACK, id)));
    }

    public void indexQuestion(QuestionBank question) {
        vectorStore.add(List.of(questionDocument(question)));
    }

    public void removeQuestion(Long id) {
        vectorStore.delete(List.of(stableId(OBJECT_TYPE_QUESTION, id)));
    }

    private void reindexTracks() {
        Filter.Expression trackFilter = new FilterExpressionBuilder()
                .eq("objectType", OBJECT_TYPE_TRACK)
                .build();
        vectorStore.delete(trackFilter);

        List<Track> tracks = trackRepository.findByIsActiveTrue();
        List<Document> docs = tracks.stream().map(this::trackDocument).toList();
        vectorStore.add(docs);
        log.info("Reindexed {} active tracks", tracks.size());
    }

    private void reindexQuestions() {
        Filter.Expression questionFilter = new FilterExpressionBuilder()
                .eq("objectType", OBJECT_TYPE_QUESTION)
                .build();
        vectorStore.delete(questionFilter);

        List<QuestionBank> questions = questionRepository.findAllActiveWithTrack();
        List<Document> docs = questions.stream().map(this::questionDocument).toList();
        vectorStore.add(docs);
        log.info("Reindexed {} active questions", questions.size());
    }

    private Document trackDocument(Track track) {
        String text = (track.getDescription() == null || track.getDescription().isBlank())
                ? track.getName()
                : track.getName() + "\n" + track.getDescription();
        return Document.builder()
                .id(stableId(OBJECT_TYPE_TRACK, track.getId()))
                .text(text)
                .metadata(Map.of(
                        "objectType", OBJECT_TYPE_TRACK,
                        "objectId", track.getId(),
                        "trackName", track.getName()))
                .build();
    }

    private Document questionDocument(QuestionBank question) {
        String text = question.getQuestionText();
        if (question.getExpectedKeywords() != null && !question.getExpectedKeywords().isBlank()) {
            text += "\n" + question.getExpectedKeywords();
        }
        if (question.getCategory() != null) {
            text += "\n" + question.getCategory();
        }
        return Document.builder()
                .id(stableId(OBJECT_TYPE_QUESTION, question.getId()))
                .text(text)
                .metadata(Map.of(
                        "objectType", OBJECT_TYPE_QUESTION,
                        "objectId", question.getId(),
                        "trackName", question.getTrack().getName()))
                .build();
    }

    private String stableId(String objectType, Long id) {
        return UUID.nameUUIDFromBytes((objectType + "-" + id).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private boolean isIndexPopulated() {
        return countDocuments() > 0;
    }

    private long countDocuments() {
        Long count = jdbcTemplate.queryForObject("SELECT count(*) FROM vector_store", Long.class);
        return count == null ? 0 : count;
    }

    private boolean embeddingConfigured() {
        return embeddingApiKey != null
                && !embeddingApiKey.isBlank()
                && !NOT_SET.equals(embeddingApiKey);
    }
}
