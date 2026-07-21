package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.config.SessionPricingConfig;
import com.careerpilot.backend.controller.advice.WalletException;
import com.careerpilot.backend.dto.request.StartSessionRequest;
import com.careerpilot.backend.dto.request.SubmitAnswerRequest;
import com.careerpilot.backend.dto.response.GeneratedQuestion;
import com.careerpilot.backend.dto.response.InterviewQuestionDto;
import com.careerpilot.backend.dto.response.InterviewSessionResponse;
import com.careerpilot.backend.dto.response.QuestionScoreResponse;
import com.careerpilot.backend.dto.response.SessionQuestionResponse;
import com.careerpilot.backend.dto.response.SessionStateResponse;
import com.careerpilot.backend.dto.response.StartSessionResponse;
import com.careerpilot.backend.dto.response.SubmitAnswerResponse;
import com.careerpilot.backend.controller.advice.SessionQuotaException;
import com.careerpilot.backend.dto.request.StartSessionRequest;
import com.careerpilot.backend.dto.request.SubmitAnswerRequest;
import com.careerpilot.backend.dto.response.GeneratedQuestion;
import com.careerpilot.backend.dto.response.InterviewQuestionDto;
import com.careerpilot.backend.dto.response.InterviewSessionResponse;
import com.careerpilot.backend.dto.response.QuestionScoreResponse;
import com.careerpilot.backend.dto.response.SessionQuestionResponse;
import com.careerpilot.backend.dto.response.SessionStateResponse;
import com.careerpilot.backend.dto.response.StartSessionResponse;
import com.careerpilot.backend.dto.response.SubmitAnswerResponse;
import com.careerpilot.backend.entity.ENUMs.SessionStatus;
import com.careerpilot.backend.entity.ENUMs.SubscriptionTier;
import com.careerpilot.backend.entity.InterviewSession;
import com.careerpilot.backend.entity.QuestionBank;
import com.careerpilot.backend.entity.SessionQuestion;
import com.careerpilot.backend.entity.Subscription;
import com.careerpilot.backend.entity.Track;
import com.careerpilot.backend.entity.User;
import com.careerpilot.backend.repository.IInterviewSessionRepository;
import com.careerpilot.backend.repository.IQuestionBankRepository;
import com.careerpilot.backend.repository.ISessionQuestionRepository;
import com.careerpilot.backend.repository.ISubscriptionRepository;
import com.careerpilot.backend.repository.ITrackRepository;
import com.careerpilot.backend.repository.IUserRepository;
import com.careerpilot.backend.service.ICoinWalletService;
import com.careerpilot.backend.service.IInterviewSessionService;
import com.careerpilot.backend.service.ILlmService;
import com.careerpilot.backend.service.IQuestionScoreService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewSessionService implements IInterviewSessionService {

  private final IInterviewSessionRepository sessionRepository;
  private final ISessionQuestionRepository sessionQuestionRepository;
  private final ISubscriptionRepository subscriptionRepository;
  private final ITrackRepository trackRepository;
  private final IUserRepository userRepository;
  private final IQuestionBankRepository questionBankRepository;
  private final ILlmService llmService;
  private final IQuestionScoreService scoreService;
  private final ICoinWalletService coinWalletService;
  private final SessionPricingConfig sessionPricingConfig;
  private final ObjectMapper objectMapper;

  // =====================================================================
  // START
  // =====================================================================

  @Override
  @Transactional
  public StartSessionResponse startSession(StartSessionRequest request, Long userId) {
    log.info("Starting session for user: {}, track: {}", userId, request.getTrackId());
    checkSessionQuota(userId);

    Track track = trackRepository.findById(request.getTrackId())
        .orElseThrow(() -> new RuntimeException("Track not found: " + request.getTrackId()));

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found: " + userId));

    int targetMinutes = request.getDurationMinutes() != null ? request.getDurationMinutes() : 15;
    int maxQuestions = request.getQuestionCount() != null ? request.getQuestionCount() : 10;

    InterviewSession session = InterviewSession.builder()
        .user(user)
        .track(track)
        .status(SessionStatus.IN_PROGRESS)
        .startedAt(LocalDateTime.now())
        .maxQuestions(maxQuestions)
        .targetDurationMinutes(targetMinutes)
        .build();
    session = sessionRepository.save(session);
    log.info("Created session ID: {} for user: {}", session.getId(), userId);

    GeneratedQuestion firstQuestion = llmService.generateNextQuestion(
        track.getId(), track.getName(), track.getDescription(), userId, List.of());
    if (firstQuestion == null) {
      throw new RuntimeException("LLM failed to generate first question for track: " + track.getName());
    }

    QuestionBank sourceQ = resolveSourceQuestion(firstQuestion.sourceQuestionId());
    SessionQuestion sq = SessionQuestion.builder()
        .session(session)
        .question(sourceQ)
        .questionText(firstQuestion.text())
        .questionOrder(1)
        .generatedByLlm(true)
        .build();
    SessionQuestion savedSq = sessionQuestionRepository.save(sq);
    log.info("Saved first question (ID: {}) for session ID: {}", savedSq.getId(), session.getId());

    return StartSessionResponse.builder()
        .sessionId(session.getId())
        .trackName(track.getName())
        .targetDurationMinutes(targetMinutes)
        .maxQuestions(maxQuestions)
        .startedAt(session.getStartedAt())
        .currentQuestion(toQuestionResponse(savedSq))
        .build();
  }

  // =====================================================================
  // SUBMIT ANSWER
  // =====================================================================

  @Override
  @Transactional
  public SubmitAnswerResponse submitAnswer(Long sessionId, SubmitAnswerRequest request, Long userId) {
    log.info("Submitting answer for session: {}, user: {}", sessionId, userId);

    InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
        .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

    if (session.getStatus() != SessionStatus.IN_PROGRESS) {
      throw new IllegalStateException(
          "Cannot submit answer to a " + session.getStatus().name().toLowerCase() + " session.");
    }

    List<SessionQuestion> questions = sessionQuestionRepository.findBySessionIdOrderByQuestionOrderAsc(sessionId);

    SessionQuestion current = questions.stream()
        .filter(q -> q.getCompletedAt() == null)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "No active question found for session " + sessionId));

    List<SubmitAnswerRequest.WordTimingDto> words = request.getWords();
    long durationMs = request.getDurationMs() != null ? request.getDurationMs() : 0L;
    double speechRateWpm = 0, avgPauseMs = 0, silenceRatio = 0;

    if (words != null && !words.isEmpty() && durationMs > 0) {
      double mins = durationMs / 60_000.0;
      speechRateWpm = words.size() / mins;
      if (words.size() > 1) {
        long totalPause = 0;
        for (int i = 1; i < words.size(); i++) {
          long gap = words.get(i).getStartMs() - words.get(i - 1).getEndMs();
          if (gap > 0)
            totalPause += gap;
        }
        avgPauseMs = (double) totalPause / (words.size() - 1);
        silenceRatio = (double) totalPause / durationMs;
      }
    }

    String wordTimingsJson = null;
    if (words != null && !words.isEmpty()) {
      try {
        wordTimingsJson = objectMapper.writeValueAsString(words);
      } catch (Exception e) {
        log.warn("Failed to serialize word timings: {}", e.getMessage());
      }
    }

    current.setUserTranscript(request.getTranscript());
    current.setDurationMs(durationMs);
    current.setWordTimingsJson(wordTimingsJson);
    current.setAudioUrl(request.getAudioUrl());
    current.setSpeechRateWpm(speechRateWpm);
    current.setAvgPauseMs(avgPauseMs);
    current.setSilenceRatio(silenceRatio);
    current.setCompletedAt(LocalDateTime.now());
    sessionQuestionRepository.save(current);

    QuestionScoreResponse scoreResponse = null;
    try {
      scoreResponse = scoreService.scoreAnswer(current);
    } catch (Exception e) {
      log.error("Scoring failed for question ID: {}. Error: {}", current.getId(), e.getMessage());
    }

    int answeredSoFar = (int) questions.stream().filter(q -> q.getCompletedAt() != null).count() + 1;
    int maxQs = session.getMaxQuestions() != null ? session.getMaxQuestions() : 10;
    int targetSecs = (session.getTargetDurationMinutes() != null ? session.getTargetDurationMinutes() : 15) * 60;

    long clientElapsed = request.getSessionElapsedSeconds() != null
        ? request.getSessionElapsedSeconds()
        : -1L;

    boolean timeUp;
    if (clientElapsed >= 0) {
      timeUp = clientElapsed >= targetSecs;
    } else {
      log.warn("Session {}: sessionElapsedSeconds not sent — skipping time check, only cap applies.", sessionId);
      timeUp = false;
    }
    boolean capReached = answeredSoFar >= maxQs;

    InterviewQuestionDto nextQuestion = null;

    if (!timeUp && !capReached) {
      List<SessionQuestion> history = sessionQuestionRepository.findBySessionIdOrderByQuestionOrderAsc(sessionId);

      GeneratedQuestion nextGq = llmService.generateNextQuestion(
          session.getTrack().getId(), session.getTrack().getName(),
          session.getTrack().getDescription(), userId, history);

      QuestionBank sourceQ = resolveSourceQuestion(nextGq.sourceQuestionId());
      SessionQuestion nextSq = SessionQuestion.builder()
          .session(session)
          .question(sourceQ)
          .questionText(nextGq.text())
          .questionOrder(answeredSoFar + 1)
          .generatedByLlm(true)
          .build();
      SessionQuestion savedNext = sessionQuestionRepository.save(nextSq);
      nextQuestion = toQuestionResponse(savedNext);
      log.info("Generated Q#{} for session {}", answeredSoFar + 1, sessionId);
    } else {
      log.info("Session {} ready to complete (timeUp={}, capReached={}). " +
          "Awaiting GET /feedback to close.", sessionId, timeUp, capReached);
    }

    session.setUpdatedAt(LocalDateTime.now());
    sessionRepository.save(session);

    String status = (timeUp || capReached) ? "READY_TO_COMPLETE" : SessionStatus.IN_PROGRESS.name();

    return SubmitAnswerResponse.builder()
        .sessionStatus(status)
        .score(scoreResponse)
        .nextQuestion(nextQuestion)
        .build();
  }

  // =====================================================================
  // STATE (network-drop recovery)
  // =====================================================================

  @Override
  @Transactional(readOnly = true)
  public SessionStateResponse getSessionState(Long sessionId, Long userId) {
    InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
        .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

    List<SessionQuestion> questions = sessionQuestionRepository.findBySessionIdOrderByQuestionOrderAsc(sessionId);

    List<SessionQuestionResponse> answered = questions.stream()
        .filter(q -> q.getCompletedAt() != null)
        .map(this::toSessionQuestionResponse)
        .collect(Collectors.toList());

    SessionQuestion current = questions.stream()
        .filter(q -> q.getCompletedAt() == null)
        .findFirst()
        .orElse(null);

    return SessionStateResponse.builder()
        .sessionId(session.getId())
        .status(session.getStatus().name())
        .trackName(session.getTrack().getName())
        .startedAt(session.getStartedAt())
        .updatedAt(session.getUpdatedAt())
        .answeredCount(answered.size())
        .totalCount(questions.size())
        .answeredQuestions(answered)
        .currentQuestion(current != null ? toQuestionResponse(current) : null)
        .build();
  }

  // =====================================================================
  // READ
  // =====================================================================

  @Override
  @Transactional(readOnly = true)
  public List<InterviewSessionResponse> listSessions(Long userId) {
    return sessionRepository.findByUserIdOrderByCreatedAtDesc(userId)
        .stream()
        .map(this::toSessionResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public InterviewSessionResponse getSession(Long sessionId, Long userId) {
    InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
        .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
    return toSessionResponse(session);
  }

  // =====================================================================
  // Helpers
  // =====================================================================

  private QuestionBank resolveSourceQuestion(Long sourceId) {
    if (sourceId == null)
      return null;
    return questionBankRepository.findById(sourceId).orElse(null);
  }

  private InterviewQuestionDto toQuestionResponse(SessionQuestion sq) {
    return InterviewQuestionDto.builder()
        .id(sq.getId())
        .sessionId(sq.getSession().getId())
        .questionText(sq.getQuestionText())
        .questionOrder(sq.getQuestionOrder())
        .createdAt(sq.getCreatedAt())
        .build();
  }

  private void checkSessionQuota(Long userId) {
    Subscription sub = subscriptionRepository.findByUserId(userId)
            .orElseGet(() -> {
              log.warn("No subscription found for user {} during quota check — treating as FREE tier", userId);
              Subscription defaultFree = new Subscription();
              defaultFree.setTier(SubscriptionTier.FREE);
              defaultFree.setFreeTrialUsed(false);
              return defaultFree;
            });

    if (sub.getTier() == SubscriptionTier.PLUS || sub.getTier() == SubscriptionTier.PRO) return;

    if (!sub.getFreeTrialUsed()) {
      long totalSessions = sessionRepository.countByUserId(userId);
      if (totalSessions == 0) {
        sub.setFreeTrialUsed(true);
        subscriptionRepository.save(sub);
        return;
      }
    }

    LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
    long monthlyCount = sessionRepository.countByUserIdAndCreatedAtAfter(userId, monthStart);

    if (monthlyCount >= 1) {
      try {
        coinWalletService.debit(userId, sessionPricingConfig.getCoinCost());
      } catch (WalletException.InsufficientBalanceException e) {
        throw new SessionQuotaException.QuotaExceededException(
                "You have 0 sessions remaining. Subscribe or buy coins.");
      }
    }
  }

  private SessionQuestionResponse toSessionQuestionResponse(SessionQuestion sq) {
    SessionQuestionResponse resp = SessionQuestionResponse.builder()
        .id(sq.getId())
        .sessionId(sq.getSession().getId())
        .questionText(sq.getQuestionText())
        .questionOrder(sq.getQuestionOrder())
        .userTranscript(sq.getUserTranscript())
        .durationMs(sq.getDurationMs())
        .speechRateWpm(sq.getSpeechRateWpm())
        .avgPauseMs(sq.getAvgPauseMs())
        .silenceRatio(sq.getSilenceRatio())
        .createdAt(sq.getCreatedAt())
        .completedAt(sq.getCompletedAt())
        .build();
    if (sq.getScore() != null) {
      resp.setScore(scoreService.getScore(sq.getId()));
    }
    return resp;
  }

  private InterviewSessionResponse toSessionResponse(InterviewSession s) {
    return InterviewSessionResponse.builder()
        .id(s.getId())
        .trackId(s.getTrack().getId())
        .trackName(s.getTrack().getName())
        .status(s.getStatus().name())
        .overallScore(s.getOverallScore())
        .durationSeconds(s.getDurationSeconds())
        .targetDurationMinutes(s.getTargetDurationMinutes())
        .maxQuestions(s.getMaxQuestions())
        .startedAt(s.getStartedAt())
        .completedAt(s.getCompletedAt())
        .createdAt(s.getCreatedAt())
        .build();
  }
}
