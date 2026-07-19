package com.careerpilot.backend.entity.ENUMs;

import lombok.Getter;

@Getter
public enum DifficultyLevel {

        EASY(0.8),
        MEDIUM(1.0),
        HARD(1.2);

        private final double weight;

        DifficultyLevel(double weight) {
                this.weight = weight;
        }

}