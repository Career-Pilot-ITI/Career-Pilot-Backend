package com.careerpilot.backend.entity.ENUMs;

import java.util.Collections;
import java.util.Map;

public enum QuestionCategory {

    BEHAVIORAL(Map.of(
            SkillCategory.TECHNICAL, 0.5,
            SkillCategory.BEHAVIORAL, 1.0,
            SkillCategory.SOFT_SKILLS, 0.8
    )),
    TECHNICAL(Map.of(
            SkillCategory.TECHNICAL, 1.0,
            SkillCategory.BEHAVIORAL, 0.5,
            SkillCategory.SOFT_SKILLS, 0.5
    )),
    PROJECT(Map.of(
            SkillCategory.TECHNICAL, 0.8,
            SkillCategory.BEHAVIORAL, 0.5,
            SkillCategory.SOFT_SKILLS, 0.5
    )),
    SITUATIONAL(Map.of(
            SkillCategory.TECHNICAL, 0.5,
            SkillCategory.BEHAVIORAL, 0.8,
            SkillCategory.SOFT_SKILLS, 0.8
    ));

    private final Map<SkillCategory, Double> alignmentWeights;

    QuestionCategory(Map<SkillCategory, Double> alignmentWeights) {
        this.alignmentWeights = Collections.unmodifiableMap(alignmentWeights);
    }

    public double alignmentWeight(SkillCategory skillCategory) {
        return alignmentWeights.getOrDefault(skillCategory, 0.5);
    }
}
