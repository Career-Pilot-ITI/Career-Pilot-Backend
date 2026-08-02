-- Spring AI PgVectorStore-managed table for track + question-bank embeddings.
-- Mirrors the DDL that PgVectorStore (1.0.0-M6) expects: id uuid, content text, metadata json, embedding vector(2048).
-- The 'vector' extension is already enabled by V1__init.sql.
CREATE TABLE IF NOT EXISTS vector_store (
    id uuid PRIMARY KEY,
    content text,
    metadata json,
    embedding vector(2048)
);

CREATE INDEX IF NOT EXISTS vector_store_hnsw_idx
    ON vector_store USING hnsw (embedding vector_cosine_ops);
