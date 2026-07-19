package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.entity.ENUMs.DifficultyLevel;
import com.careerpilot.backend.entity.ENUMs.QuestionCategory;
import com.careerpilot.backend.entity.ENUMs.SkillCategory;
import com.careerpilot.backend.entity.QuestionBank;
import com.careerpilot.backend.entity.SessionQuestion;
import com.careerpilot.backend.entity.User;
import com.careerpilot.backend.entity.UserProfile;
import com.careerpilot.backend.entity.UserSkill;
import com.careerpilot.backend.repository.IUserProfileRepository;
import com.careerpilot.backend.repository.UserSkillRepository;
import com.careerpilot.backend.service.IUserSkillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSkillServiceImpl implements IUserSkillService {

    private static final int JUNIOR_THRESHOLD = 2;
    private static final int SENIOR_THRESHOLD = 5;

    private final UserSkillRepository userSkillRepository;
    private final IUserProfileRepository userProfileRepository;

    @Override
    @Transactional
    public void updateSkillsFromScore(SessionQuestion sessionQuestion, int overallScore) {
        QuestionBank questionBank = sessionQuestion.getQuestion();
        if (questionBank == null) {
            log.debug("No QuestionBank linked to session question {} — skipping skill assessment",
                    sessionQuestion.getId());
            return;
        }

        String keywords = questionBank.getExpectedKeywords();
        if (keywords == null || keywords.isBlank()) {
            log.debug("No expectedKeywords for question bank ID {} — skipping skill assessment",
                    questionBank.getId());
            return;
        }

        Long userId = sessionQuestion.getSession().getUser().getId();
        User user = sessionQuestion.getSession().getUser();

        DifficultyLevel difficulty = questionBank.getDifficultyLevel();
        if (difficulty == null) difficulty = DifficultyLevel.MEDIUM;
        double difficultyWeight = difficulty.getWeight();

        QuestionCategory questionCategory = questionBank.getCategory();
        if (questionCategory == null) questionCategory = QuestionCategory.TECHNICAL;

        double seniorityWeight = computeSeniorityWeight(userId, difficulty);

        String[] skills = keywords.split(",");
        for (String raw : skills) {
            String skillName = raw.trim();
            if (skillName.isEmpty()) continue;

            Optional<UserSkill> existing = userSkillRepository
                    .findByUserIdAndSkillNameIgnoreCase(userId, skillName);

            UserSkill userSkill;
            if (existing.isPresent()) {
                userSkill = existing.get();
            } else {
                userSkill = new UserSkill();
                userSkill.setUser(user);
                userSkill.setSkillName(skillName);
                userSkill.setCategory(SkillCategory.TECHNICAL);
                userSkill.setPerformanceScore(0);
                userSkill.setTimesAssessed(0);
                userSkill.setCreatedAt(LocalDateTime.now());
                userSkill.setUpdatedAt(LocalDateTime.now());
            }

            double categoryAlignment = questionCategory.alignmentWeight(userSkill.getCategory());

            double effectiveWeight = difficultyWeight * seniorityWeight * categoryAlignment;
            int weightedScore = (int) Math.round(Math.min(100, Math.max(0, overallScore * effectiveWeight)));

            int oldTimes = userSkill.getTimesAssessed();
            int oldScore = userSkill.getPerformanceScore() != null
                    ? userSkill.getPerformanceScore() : 0;
            int newScore = ((oldScore * oldTimes) + weightedScore) / (oldTimes + 1);

            userSkill.setPerformanceScore(newScore);
            userSkill.setTimesAssessed(oldTimes + 1);
            userSkill.setLastAssessedAt(LocalDateTime.now());
            userSkill.setUpdatedAt(LocalDateTime.now());

            userSkillRepository.save(userSkill);

            log.debug("Updated UserSkill '{}' for user {}: {} (raw={}, weight={})",
                    skillName, userId, newScore, overallScore,
                    String.format("%.2f", effectiveWeight));
        }
    }

    private double computeSeniorityWeight(Long userId, DifficultyLevel questionDifficulty) {
        Optional<UserProfile> profile = userProfileRepository.findByUserId(userId);
        int years = profile.map(UserProfile::getYearsOfExperience).orElse(0);

        UserLevel userLevel;
        if (years < JUNIOR_THRESHOLD) {
            userLevel = UserLevel.JUNIOR;
        } else if (years < SENIOR_THRESHOLD) {
            userLevel = UserLevel.MID;
        } else {
            userLevel = UserLevel.SENIOR;
        }

        UserLevel questionLevel = switch (questionDifficulty) {
            case EASY -> UserLevel.JUNIOR;
            case MEDIUM -> UserLevel.MID;
            case HARD -> UserLevel.SENIOR;
        };

        int comparison = userLevel.ordinal() - questionLevel.ordinal();
        if (comparison < 0) return 1.3;
        if (comparison == 0) return 1.0;
        return 0.7;
    }

    private enum UserLevel {
        JUNIOR, MID, SENIOR
    }
}
