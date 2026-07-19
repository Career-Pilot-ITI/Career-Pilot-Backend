package com.careerpilot.backend.repository;

import com.careerpilot.backend.entity.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IUserSkillRepository extends JpaRepository<UserSkill, Long> {
    List<UserSkill> findByUserId(Long userId);

    Optional<UserSkill> findByUserIdAndSkillName(Long userId, String skillName);

    @Query("SELECT us FROM UserSkill us WHERE LOWER(us.skillName) = LOWER(:skillName) AND us.user.id = :userId")
    Optional<UserSkill> findByUserIdAndSkillNameIgnoreCase(@Param("userId") Long userId, @Param("skillName") String skillName);

    void deleteByUserId(Long userId);
}
