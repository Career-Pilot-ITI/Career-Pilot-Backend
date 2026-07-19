package com.careerpilot.backend.service;

import com.careerpilot.backend.entity.SessionQuestion;

public interface IUserSkillService {

    void updateSkillsFromScore(SessionQuestion sessionQuestion, int overallScore);
}
