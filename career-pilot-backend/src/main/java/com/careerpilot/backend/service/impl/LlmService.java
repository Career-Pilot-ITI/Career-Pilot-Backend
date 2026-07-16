package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.service.ILlmService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * LLM Service implementation using Google Gemini API (generateContent endpoint).
 *
 * Endpoint: POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={apiKey}
 * Docs: https://ai.google.dev/api/generate-content
 *
 * Falls back to heuristic scoring when:
 *   - app.llm.enabled=false (default)
 *   - app.llm.api-key is blank
 *   - API call fails for any reason
 */
@Service
@Slf4j
public class LlmService implements ILlmService {

    private static final String GEMINI_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    @Value("${app.llm.api-key:}")
    private String apiKey;

    @Value("${app.llm.model:gemini-1.5-flash}")
    private String model;

    @Value("${app.llm.enabled:false}")
    private boolean llmEnabled;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public LlmScoreResult scoreAnswer(String questionText, String transcript) {
        if (!llmEnabled || apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini scoring is disabled or API key not configured. Returning heuristic scores.");
            return heuristicScores(transcript);
        }

        try {
            String prompt = buildPrompt(questionText, transcript);
            String url = String.format(GEMINI_BASE_URL, model, apiKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Gemini generateContent request body
            Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                    Map.of(
                        "parts", List.of(
                            Map.of("text", getSystemInstructions() + "\n\n" + prompt)
                        )
                    )
                ),
                "generationConfig", Map.of(
                    "temperature", 0.1,
                    "responseMimeType", "application/json"
                )
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            return parseGeminiResponse(response.getBody());

        } catch (Exception e) {
            log.error("Gemini API call failed, falling back to heuristic scores. Error: {}", e.getMessage());
            return heuristicScores(transcript);
        }
    }

    // ---- Private helpers ----

    private String getSystemInstructions() {
        return """
                You are an expert interview coach evaluating a candidate's spoken answer.
                
                Score the answer on 4 dimensions (integers 0-100 each):
                - content_relevance: How directly and completely the answer addresses the question.
                  (0 = completely off-topic, 100 = perfectly on-point and thorough)
                - clarity: Structure, coherence, vocabulary, and ease of understanding.
                  (0 = incoherent, 100 = crystal clear and well-structured)
                - confidence: Assertiveness, use of hedging language ("I think maybe"), directness.
                  (0 = very uncertain/passive, 100 = highly confident and assertive)
                - filler_words: Inverse filler-word score. Fillers include: um, uh, like, you know, basically, actually, sort of.
                  (0 = constant fillers making it hard to follow, 100 = no filler words at all)
                
                You MUST respond with ONLY a valid JSON object — no markdown, no explanation, no backticks:
                {"content_relevance": <int>, "clarity": <int>, "confidence": <int>, "filler_words": <int>}
                """;
    }

    private String buildPrompt(String questionText, String transcript) {
        return String.format("""
                Interview Question:
                %s
                
                Candidate's Answer (from speech-to-text):
                %s
                
                Score the answer on all 4 dimensions. Return ONLY the JSON object.
                """, questionText, transcript);
    }

    /**
     * Parse Gemini API response.
     * Response structure:
     * { "candidates": [{ "content": { "parts": [{ "text": "{...json...}" }] } }] }
     */
    private LlmScoreResult parseGeminiResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String text = root
                .path("candidates")
                .get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text")
                .asText();

        // Clean up potential markdown fencing from model output
        text = text.strip();
        if (text.startsWith("```")) {
            text = text.replaceAll("```json\\s*|```\\s*", "").strip();
        }

        JsonNode scores = objectMapper.readTree(text);
        return new LlmScoreResult(
                clamp(scores.path("content_relevance").asInt(50)),
                clamp(scores.path("clarity").asInt(50)),
                clamp(scores.path("confidence").asInt(50)),
                clamp(scores.path("filler_words").asInt(50))
        );
    }

    /**
     * Fallback heuristic scoring — used when LLM is disabled or the call fails.
     * Produces reasonable estimates from transcript length and filler word count.
     */
    private LlmScoreResult heuristicScores(String transcript) {
        if (transcript == null || transcript.isBlank()) {
            return new LlmScoreResult(0, 0, 0, 100);
        }

        String[] words = transcript.trim().split("\\s+");
        int wordCount = words.length;
        String lower = transcript.toLowerCase();

        int contentScore = Math.min(100, 20 + wordCount * 2);  // more words → more content (rough)
        int clarityScore = wordCount >= 30 ? 65 : (wordCount >= 15 ? 50 : 35);
        int confidenceScore = 55; // neutral default

        // Count common filler words
        long fillerCount = List.of("um", "uh", "like", "you know", "basically", "actually", "sort of", "kind of")
                .stream()
                .mapToLong(filler -> countOccurrences(lower, filler))
                .sum();
        int fillerScore = (int) Math.max(0, 100 - (fillerCount * 15));

        return new LlmScoreResult(
                clamp(contentScore),
                clamp(clarityScore),
                clamp(confidenceScore),
                clamp(fillerScore)
        );
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private long countOccurrences(String text, String phrase) {
        long count = 0;
        int idx = 0;
        while ((idx = text.indexOf(phrase, idx)) != -1) {
            count++;
            idx += phrase.length();
        }
        return count;
    }
}
