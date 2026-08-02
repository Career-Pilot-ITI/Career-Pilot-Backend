package com.careerpilot.backend.embedding;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Holds shared embedding constants and provides a lightweight
 * utility to count indexed documents by type in the vector store.
 */
@Component
@RequiredArgsConstructor
public class VectorState {

    public static final String OBJECT_TYPE_TRACK    = "TRACK";
    public static final String OBJECT_TYPE_QUESTION = "QUESTION";

    private final JdbcTemplate jdbcTemplate;

    public long countByType(String objectType) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM vector_store WHERE metadata->>'objectType' = ?",
                Long.class, objectType);
        return count == null ? 0 : count;
    }
}
