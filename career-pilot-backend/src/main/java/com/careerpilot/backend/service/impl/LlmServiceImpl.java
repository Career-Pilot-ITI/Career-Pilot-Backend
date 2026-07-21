package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.service.ILlmService;

import com.careerpilot.backend.dto.response.CvAnalysis;
import com.careerpilot.backend.dto.response.GeneratedQuestion;
import com.careerpilot.backend.dto.response.ScoreResponse;
import com.careerpilot.backend.entity.ENUMs.DocType;
import com.careerpilot.backend.entity.FeedbackReport;
import com.careerpilot.backend.entity.QuestionBank;
import com.careerpilot.backend.entity.QuestionScore;
import com.careerpilot.backend.entity.RagContextDocument;
import com.careerpilot.backend.entity.SessionQuestion;

import com.careerpilot.backend.repository.IFeedbackReportRepository;
import com.careerpilot.backend.repository.IQuestionBankRepository;
import com.careerpilot.backend.repository.IQuestionScoreRepository;
import com.careerpilot.backend.repository.IRagContextDocumentRepository;
import com.careerpilot.backend.repository.ISessionQuestionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.careerpilot.backend.utils.PiiRedactionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmServiceImpl implements ILlmService {

  private final ChatClient chatClient;
  private final ObjectMapper objectMapper;
  private final IQuestionBankRepository questionBankRepository;
  private final IFeedbackReportRepository feedbackReportRepository;
  private final IRagContextDocumentRepository ragContextDocumentRepository;
  private final ISessionQuestionRepository sessionQuestionRepository;
  private final IQuestionScoreRepository questionScoreRepository;

  @Override
  public GeneratedQuestion generateNextQuestion(Long trackId, String trackName, String trackDescription, Long userId, List<SessionQuestion> previousQuestions) {
    List<QuestionBank> baseQuestions = questionBankRepository.findByTrackIdAndIsActiveTrue(trackId);
    if (baseQuestions.isEmpty()) {
      baseQuestions = questionBankRepository.findByTrackId(trackId);
    }
    String samples = baseQuestions.stream()
        .map(q -> "- ID " + q.getId() + ": " + q.getQuestionText()
            + "  [keywords: " + q.getExpectedKeywords() + "]")
        .collect(Collectors.joining("\n"));

    StringBuilder historySb = new StringBuilder();
    if (previousQuestions == null || previousQuestions.isEmpty()) {
      historySb.append("No questions asked yet. This is the beginning of the interview. You must generate the first, opening question.\n");
    } else {
      historySb.append("Here is the conversation history of the interview so far:\n");
      for (SessionQuestion sq : previousQuestions) {
        historySb.append("Interviewer: ").append(sq.getQuestionText()).append("\n");
        historySb.append("Candidate: ").append(sq.getUserTranscript() != null ? sq.getUserTranscript() : "[No response/skipped]").append("\n");
        historySb.append("\n");
      }
    }

    String cvText = buildCvContext(userId);
    String desc = (trackDescription != null && !trackDescription.isBlank()) ? trackDescription : "Assess the candidate's skills for this role.";
    String cvContext = (cvText != null && !cvText.isBlank()) ? cvText : "No CV provided.";

    String prompt = """
        You are conducting a dynamic, interactive, and open-ended interview for the track: "%s".
        
        Track Objective: %s
        
        Candidate CV Context:
        %s
        
        Sample Reference Questions for this Track (use these for concepts/keywords targets and difficulty calibration):
        %s
        
        %s
        
        Based on the track objective, the CV, and the history, generate the NEXT question.
        
        Guidelines:
        1. If this is the start (no history), generate a friendly but professional open-ended introductory/technical question tailored to their track and CV.
        2. If there is history:
           - Evaluate the candidate's last response.
           - If they left room for clarification, made an interesting technical claim, or did not fully cover the concept, generate an adaptive FOLLOW-UP question (e.g., "You mentioned using X to solve Y, how would you handle Z in that scenario?").
           - If their answer was complete, transition to a new topic testing another competency from the track sample questions.
        3. Ensure the question is open-ended, realistic, and directly tests their actual capabilities.
        4. Anchor every question to the track objective above — do not drift into unrelated topics.
        
        Return ONLY a JSON object with no markdown formatting and no extra text. Format:
        {"text": "your question text here", "sourceQuestionId": null}
        (Note: set "sourceQuestionId" to the ID of a sample question if it tests the exact same concept/keywords, or null if it is a custom follow-up).
        """.formatted(trackName, desc, cvContext, samples, historySb.toString());

    String response = chatClient.prompt()
        .system(s -> s.text("""
            You are an expert interviewer.
            Generate a single, natural, open-ended question or follow-up question.
            Return ONLY the JSON object.
            """))
        .user(prompt)
        .call()
        .content();

    try {
      return objectMapper.readValue(stripMarkdown(response), GeneratedQuestion.class);
    } catch (Exception e) {
      log.warn("Failed to parse next question LLM response: {}", response, e);
      if (!baseQuestions.isEmpty()) {
        QuestionBank randomQ = baseQuestions.get((int) (Math.random() * baseQuestions.size()));
        return new GeneratedQuestion(randomQ.getQuestionText(), randomQ.getId());
      }
      return new GeneratedQuestion("Can you describe your experience as a " + trackName + "?", null);
    }
  }

  @Override
  public ScoreResponse scoreAnswer(Long questionId, Long userId, String transcript) {
    String idealAnswer = buildIdealAnswerContext(questionId);
    String history = buildUserHistoryContext(userId);
    String cv = buildCvContext(userId);
    String sessionCtx = buildSessionContext(userId);

    String prompt = """
        Score this interview answer.

        Question being answered: %s

        Ideal answer reference:
        %s

        Candidate's past performance:
        %s

        Candidate's CV context:
        %s

        Session context:
        %s

        Candidate answer: %s

        Score 0-100 on:
        - Content Relevance: does the answer cover the expected keywords and concepts?
        - Clarity: well-structured, coherent, grammatical?
        - Confidence: hedging language? ("I think", "maybe", self-correction)
        - Filler words: count of "um/uh/like/you know"

        Use the ideal answer reference to determine if the candidate hit the key points.
        Use the past performance and CV to calibrate expectations.

        Return ONLY raw JSON with no markdown formatting.
        {"contentRelevance": 0, "clarity": 0, "confidence": 0, "fillerWords": 0, "reasoning": "..."}
        """.formatted(questionId, idealAnswer, history, cv, sessionCtx, transcript);

    String response = chatClient.prompt()
        .system(s -> s.text(
            """
                You are an expert interview evaluator with 15+ years of experience.
                Score answers against the ideal answer keywords. Be critical — a score of 100 means the answer was perfect, which is rare.
                """))
        .user(prompt)
        .call()
        .content();

    try {
      return objectMapper.readValue(stripMarkdown(response), ScoreResponse.class);
    } catch (Exception e) {
      log.warn("Failed to parse score response: {}", response, e);
      return new ScoreResponse(0, 0, 0, 0, "Could not evaluate answer");
    }
  }

  @Override
  public List<String> generateSessionTips(Long sessionId, Long userId) {
    List<SessionQuestion> questions = sessionQuestionRepository
        .findBySessionIdOrderByQuestionOrderAsc(sessionId);

    List<Long> questionIds = questions.stream()
        .map(SessionQuestion::getId)
        .collect(Collectors.toList());

    Map<Long, QuestionScore> scoreByQuestionId = questionScoreRepository
        .findBySessionQuestionIdIn(questionIds).stream()
        .collect(Collectors.toMap(score -> score.getSessionQuestion().getId(), score -> score));

    StringBuilder sb = new StringBuilder();
    sb.append("Here is the full interview session. Generate 3-5 overall coaching tips.\n\n");

    for (SessionQuestion q : questions) {
      sb.append("--- Question ").append(q.getQuestionOrder()).append(" ---\n");
      sb.append("Question text: ").append(q.getQuestionText()).append("\n");
      sb.append("Candidate answer: ").append(q.getUserTranscript()).append("\n");

      QuestionScore sc = scoreByQuestionId.get(q.getId());
      if (sc != null) {
        sb.append("Scores: contentRelevance=").append(sc.getContentRelevance())
            .append(", clarity=").append(sc.getClarity())
            .append(", confidence=").append(sc.getConfidence())
            .append(", pacing=").append(sc.getPacing())
            .append(", fillerWords=").append(sc.getFillerWords())
            .append(", overall=").append(sc.getOverallScore()).append("\n");
      }
      sb.append("\n");
    }

    String prompt = sb.toString() + """
        Return ONLY a JSON array of strings with no markdown formatting.
        Example: ["Focus on structuring your answer with STAR method", "Work on reducing filler words"]
        Each tip must be a concrete, actionable recommendation based on the patterns visible in the transcripts and scores.
        """;

    String response = chatClient.prompt()
        .system(s -> s.text("""
            You are a senior career coach helping candidates improve their interview performance.
            Provide specific, actionable, and constructive feedback.
            Each tip should be a concrete recommendation, not generic advice.
            Focus on the patterns across the entire session.
            """))
        .user(prompt)
        .call()
        .content();

    try {
      return objectMapper.readValue(stripMarkdown(response), new TypeReference<List<String>>() {});
    } catch (Exception e) {
      log.warn("Failed to parse session tips response: {}", response, e);
      return List.of("Review your answers and focus on clarity and structure.");
    }
  }

  @Override
  public String generateQuestionTip(String questionText, String transcript,
      int contentRelevance, int clarity, int confidence, int pacing, int fillerWords) {
    String prompt = """
        Based on this interview answer and scores, generate a brief coaching tip or praise (1-2 sentences).

        Question: %s
        Candidate answer: %s

        Scores:
        - Content Relevance: %s/100
        - Clarity: %s/100
        - Confidence: %s/100
        - Pacing: %s/100
        - Filler words: %s/100

        If the overall score is high (80+), praise the candidate and suggest one minor improvement.
        If the overall score is low, give one specific, actionable tip to improve.
        Return ONLY the tip text, no JSON, no markdown.
        """.formatted(questionText, transcript,
        contentRelevance, clarity, confidence, pacing, fillerWords);

    String response = chatClient.prompt()
        .system(s -> s.text("""
            You are a senior career coach giving real-time feedback after each interview question.
            Be concise (1-2 sentences). Be specific. Be encouraging even when being critical.
            """))
        .user(prompt)
        .call()
        .content();

    return response != null ? response.strip() : "Keep practicing to improve your answer.";
  }

  @Override
  public CvAnalysis analyzeCv(String cvText) {
    String prompt = """
        Extract structured information from this CV text.

        CV: %s

        Return ONLY raw JSON with no markdown formatting.
        {"skills": [], "yearsOfExperience": 0, "targetRole": "", "educationLevel": "", "displayName": "", "currentJobTitle": ""}
        """
        .formatted(cvText);

    String response = chatClient.prompt()
        .system(s -> s.text("""
            You are an HR expert specializing in CV parsing and skills assessment.
            Extract structured information from unstructured CV text accurately.
            If a field cannot be determined, use reasonable defaults or empty lists.
            """))
        .user(prompt)
        .call()
        .content();

    try {
      return objectMapper.readValue(stripMarkdown(response), CvAnalysis.class);
    } catch (Exception e) {
      log.warn("Failed to parse CV analysis response: {}", response, e);
      return new CvAnalysis();
    }
  }

  private String buildIdealAnswerContext(Long questionId) {
    Optional<QuestionBank> opt = questionBankRepository.findById(questionId);
    if (opt.isEmpty())
      return "No reference answer available.";
    QuestionBank q = opt.get();
    StringBuilder sb = new StringBuilder();
    sb.append("Question: ").append(q.getQuestionText()).append("\n");
    if (q.getExpectedKeywords() != null && !q.getExpectedKeywords().isBlank())
      sb.append("Expected keywords: ").append(q.getExpectedKeywords()).append("\n");
    sb.append("Difficulty: ").append(q.getDifficultyLevel()).append("\n");
    sb.append("Category: ").append(q.getCategory());
    return sb.toString();
  }

  private String buildUserHistoryContext(Long userId) {
    List<FeedbackReport> reports = feedbackReportRepository
        .findBySessionUserIdOrderByCreatedAtDesc(userId);
    if (reports.isEmpty())
      return "No past session history available.";

    double avgScore = reports.stream()
        .mapToInt(FeedbackReport::getOverallScore)
        .average()
        .orElse(0);

    List<String> tips = reports.stream()
        .filter(r -> r.getCoachingTips() != null)
        .map(FeedbackReport::getCoachingTips)
        .toList();

    StringBuilder sb = new StringBuilder();
    sb.append("Past sessions: ").append(reports.size()).append("\n");
    sb.append("Average score: ").append(String.format("%.1f", avgScore)).append("\n");
    if (!tips.isEmpty()) {
      sb.append("Recurring advice:\n");
      tips.stream().limit(3).forEach(t -> sb.append("- ").append(t).append("\n"));
    }
    return sb.toString();
  }

  private String buildCvContext(Long userId) {
    List<RagContextDocument> docs = ragContextDocumentRepository
        .findByUserIdAndDocTypeOrderByCreatedAtDesc(userId, DocType.CV_EXTRACT);
    if (docs.isEmpty())
      return "No CV context available.";
    return PiiRedactionUtil.redact(docs.get(0).getContent());
  }

  private String buildSessionContext(Long userId) {
    List<RagContextDocument> docs = ragContextDocumentRepository
        .findByUserIdAndDocTypeOrderByCreatedAtDesc(userId, DocType.SESSION_SUMMARY);
    if (docs.isEmpty())
      return "No session context available.";
    return PiiRedactionUtil.redact(docs.stream()
        .limit(3)
        .map(RagContextDocument::getContent)
        .collect(Collectors.joining("\n---\n")));
  }

  private String stripMarkdown(String raw) {
    if (raw == null)
      return "{}";
    return raw.replaceAll("```(?:json)?\\s*", "").trim();
  }
}
