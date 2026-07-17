package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.dto.request.StartSessionRequest;
import com.careerpilot.backend.dto.response.InterviewSessionResponse;
import com.careerpilot.backend.dto.response.SessionQuestionResponse;
import com.careerpilot.backend.entity.ENUMs.SessionStatus;
import com.careerpilot.backend.entity.InterviewSession;
import com.careerpilot.backend.entity.QuestionBank;
import com.careerpilot.backend.entity.SessionQuestion;
import com.careerpilot.backend.entity.Track;
import com.careerpilot.backend.entity.User;
import com.careerpilot.backend.repository.IInterviewSessionRepository;
import com.careerpilot.backend.repository.IQuestionBankRepository;
import com.careerpilot.backend.repository.ISessionQuestionRepository;
import com.careerpilot.backend.repository.ITrackRepository;
import com.careerpilot.backend.repository.IUserRepository;
import com.careerpilot.backend.service.IInterviewSessionService;
import com.careerpilot.backend.service.IQuestionScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewSessionService implements IInterviewSessionService {

    private static final int MAX_QUESTIONS_PER_SESSION = 5;

    private final IInterviewSessionRepository sessionRepository;
    private final ISessionQuestionRepository sessionQuestionRepository;
    private final IQuestionBankRepository questionBankRepository;
    private final ITrackRepository trackRepository;
    private final IUserRepository userRepository;
    private final IQuestionScoreService scoreService;

    @Override
    @Transactional
    public InterviewSessionResponse startSession(StartSessionRequest request, Long userId) {
        log.info("Starting session for user: {}, track: {}", userId, request.getTrackId());

        // 1. Validate track
        Track track = trackRepository.findById(request.getTrackId())
                .orElseThrow(() -> new RuntimeException("Track not found: " + request.getTrackId()));

        // 2. Load user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // 3. Pick questions — up to MAX_QUESTIONS_PER_SESSION active questions, shuffled
        List<QuestionBank> pool = questionBankRepository.findByTrackIdAndIsActiveTrue(request.getTrackId());
        if (pool.isEmpty()) {
            throw new RuntimeException("No active questions found for track: " + track.getName());
        }
        Collections.shuffle(pool);
        List<QuestionBank> selected = pool.subList(0, Math.min(MAX_QUESTIONS_PER_SESSION, pool.size()));

        // 4. Create the session
        InterviewSession session = InterviewSession.builder()
                .user(user)
                .track(track)
                .status(SessionStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .build();
        session = sessionRepository.save(session);
        log.info("Created session ID: {} for user: {}", session.getId(), userId);

        // 5. Create ordered SessionQuestion entries
        final InterviewSession savedSession = session;
        List<SessionQuestion> sessionQuestions = IntStream.range(0, selected.size())
                .mapToObj(i -> {
                    QuestionBank q = selected.get(i);
                    return SessionQuestion.builder()
                            .session(savedSession)
                            .question(q)
                            .questionText(q.getQuestionText())
                            .questionOrder(i + 1)
                            .build();
                })
                .collect(Collectors.toList());

        sessionQuestionRepository.saveAll(sessionQuestions);
        log.info("Attached {} questions to session ID: {}", sessionQuestions.size(), savedSession.getId());

        return mapToResponse(savedSession, sessionQuestions);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewSessionResponse> listSessions(Long userId) {
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(s -> {
                    List<SessionQuestion> qs =
                            sessionQuestionRepository.findBySessionIdOrderByQuestionOrderAsc(s.getId());
                    return mapToResponse(s, qs);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewSessionResponse getSession(Long sessionId, Long userId) {
        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        List<SessionQuestion> questions =
                sessionQuestionRepository.findBySessionIdOrderByQuestionOrderAsc(sessionId);
        return mapToResponse(session, questions);
    }

    @Override
    @Transactional
    public InterviewSessionResponse completeSession(Long sessionId, Long userId) {
        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new IllegalStateException("Session is already completed.");
        }
        if (session.getStatus() == SessionStatus.ABANDONED) {
            throw new IllegalStateException("Cannot complete an abandoned session.");
        }

        session.setStatus(SessionStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());

        // Compute duration in seconds
        if (session.getStartedAt() != null) {
            long seconds = java.time.Duration.between(session.getStartedAt(), session.getCompletedAt()).getSeconds();
            session.setDurationSeconds((int) seconds);
        }

        InterviewSession saved = sessionRepository.save(session);
        log.info("Completed session ID: {} for user: {}", sessionId, userId);

        List<SessionQuestion> questions =
                sessionQuestionRepository.findBySessionIdOrderByQuestionOrderAsc(sessionId);
        return mapToResponse(saved, questions);
    }

    // ---- Mapper ----

    private InterviewSessionResponse mapToResponse(InterviewSession session, List<SessionQuestion> questions) {
        List<SessionQuestionResponse> questionResponses = questions.stream()
                .map(sq -> {
                    SessionQuestionResponse resp = SessionQuestionResponse.builder()
                            .id(sq.getId())
                            .sessionId(session.getId())
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
                    resp.setScore(scoreService.getScore(sq.getId()));
                    return resp;
                })
                .collect(Collectors.toList());

        return InterviewSessionResponse.builder()
                .id(session.getId())
                .trackId(session.getTrack().getId())
                .trackName(session.getTrack().getName())
                .status(session.getStatus().name())
                .overallScore(session.getOverallScore())
                .durationSeconds(session.getDurationSeconds())
                .createdAt(session.getCreatedAt())
                .startedAt(session.getStartedAt())
                .completedAt(session.getCompletedAt())
                .questions(questionResponses)
                .build();
    }
}
