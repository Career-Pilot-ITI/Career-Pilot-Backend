package com.careerpilot.backend.repository;

import com.careerpilot.backend.entity.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {
    Optional<UserSkill> findByUserIdAndSkillName(Long userId, String skillName);
    List<UserSkill> findByUserId(Long userId);
}