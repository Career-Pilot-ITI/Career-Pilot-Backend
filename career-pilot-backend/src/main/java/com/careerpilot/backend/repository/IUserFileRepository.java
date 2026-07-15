package com.careerpilot.backend.repository;

import com.careerpilot.backend.entity.UserFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IUserFileRepository extends JpaRepository<UserFile, Long> {
    List<UserFile> findByUserId(Long userId);
    Optional<UserFile> findByIdAndUserId(Long id, Long userId);
}
