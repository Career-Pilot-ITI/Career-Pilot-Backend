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

    private static final long REINDEX_LOCK_KEY = 73317331L;

    private final JdbcTemplate jdbcTemplate;

    public long countByType(String objectType) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM vector_store WHERE metadata->>'objectType' = ?",
                Long.class, objectType);
        return count == null ? 0 : count;
    }

    /**
     * Removes vector documents whose source row no longer exists or is inactive.
     * Used during reindex so interrupted runs never leave stale or orphaned embeddings.
     */
    public long removeStale(String objectType, String sourceTable) {
        String sql = """
                DELETE FROM vector_store vs
                WHERE vs.metadata->>'objectType' = ?
                  AND (
                    SELECT t.is_active FROM %s t
                    WHERE t.id = CAST(vs.metadata->>'objectId' AS BIGINT)
                  ) IS DISTINCT FROM TRUE
                """.formatted(sourceTable);
        return jdbcTemplate.update(sql, objectType);
    }

    /**
     * Advisory lock so concurrent instances (rolling deploys) never run the
     * reindex twice at the same time. The lock is auto-released if the pod dies.
     */
    public boolean tryAcquireReindexLock() {
        Boolean acquired = jdbcTemplate.queryForObject(
                "SELECT pg_try_advisory_lock(CAST(? AS BIGINT))", Boolean.class, REINDEX_LOCK_KEY);
        return Boolean.TRUE.equals(acquired);
    }

    public void releaseReindexLock() {
        jdbcTemplate.update("SELECT pg_advisory_unlock(CAST(? AS BIGINT))", REINDEX_LOCK_KEY);
    }
}
