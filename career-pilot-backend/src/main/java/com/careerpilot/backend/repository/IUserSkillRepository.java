package com.careerpilot.backend.repository;

import com.careerpilot.backend.entity.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IUserSkillRepository extends JpaRepository<UserSkill, Long> {
    List<UserSkill> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
