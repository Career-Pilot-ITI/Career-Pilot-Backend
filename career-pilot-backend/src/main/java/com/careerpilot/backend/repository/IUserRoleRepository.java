package com.careerpilot.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.careerpilot.backend.entity.UserRole;


@Repository
public interface IUserRoleRepository extends JpaRepository<UserRole, Long> {
}
