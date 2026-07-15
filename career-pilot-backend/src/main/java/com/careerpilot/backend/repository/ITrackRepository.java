package com.careerpilot.backend.repository;

import com.careerpilot.backend.entity.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ITrackRepository extends JpaRepository<Track, Long> {
    Optional<Track> findByName(String name);
    boolean existsByName(String name);
}