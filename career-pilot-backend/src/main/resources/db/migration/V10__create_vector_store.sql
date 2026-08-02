-- Spring AI PgVectorStore-managed table for track + question-bank embeddings.
-- Mirrors the DDL that PgVectorStore (1.0.0-M6) expects: id uuid, content text, metadata json, embedding vector(D).
-- The 'vector' extension is already enabled by V1__init.sql.
--
-- Dimension is 1536 (not the model's native 2048): llama-nemotron-embed-vl supports
-- Matryoshka (MRL) truncation via the "dimensions" param, and pgvector approximate
-- indexes (HNSW/IVFFLAT) are capped at 2000 dimensions. 1536 keeps an HNSW index
-- (and thus indexed ANN search) while retaining most retrieval quality.
CREATE TABLE IF NOT EXISTS vector_store (
    id uuid PRIMARY KEY,
    content text,
    metadata json,
    embedding vector(1536)
);

CREATE INDEX IF NOT EXISTS vector_store_hnsw_idx
    ON vector_store USING hnsw (embedding vector_cosine_ops);
