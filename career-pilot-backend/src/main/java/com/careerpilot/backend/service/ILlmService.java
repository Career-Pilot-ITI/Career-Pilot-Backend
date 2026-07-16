package com.careerpilot.backend.service;

/**
 * LLM service for question generation, scoring, and feedback generation.
 * Interacts with an external LLM (e.g., OpenAI GPT / Google Gemini).
 */
public interface ILlmService {

    /**
     * Score a candidate's answer to an interview question on 4 dimensions.
     *
     * @param questionText the interview question
     * @param transcript   the candidate's spoken answer (STT output)
     * @return LlmScoreResult with scores 0-100 for each dimension
     */
    LlmScoreResult scoreAnswer(String questionText, String transcript);

    /**
     * Result returned from LLM scoring.
     * Content relevance is scored separately; pacing is computed server-side from word timings.
     */
    record LlmScoreResult(
            int contentRelevance,  // 0-100: how well the answer addresses the question
            int clarity,           // 0-100: structure, coherence, vocabulary
            int confidence,        // 0-100: assertive language, hedging, directness
            int fillerWords        // 0-100: inverse score (fewer fillers → higher score)
    ) {}
}
