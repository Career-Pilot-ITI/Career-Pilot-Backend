package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.controller.response.TrackResponse;
import com.careerpilot.backend.dto.request.CreateTrackRequest;
import com.careerpilot.backend.dto.request.UpdateTrackRequest;
import com.careerpilot.backend.entity.Track;
import com.careerpilot.backend.repository.ITrackRepository;
import com.careerpilot.backend.service.ITrackService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackService implements ITrackService {
  private final ITrackRepository trackRepository;

  @Override
  @Transactional(readOnly = true)
  public Page<TrackResponse> findAll(Pageable pageable) {
    log.debug("Fetching all tracks with pagination: page {}, size {}", pageable.getPageNumber(),
        pageable.getPageSize());
    return trackRepository.findAll(pageable).map(TrackResponse::from);
  }

  @Override
  @Transactional(readOnly = true)
  public TrackResponse findById(Long id) {
    log.debug("Finding track with ID: {}", id);
    return trackRepository.findById(id).map(TrackResponse::from)
        .orElseThrow(() -> new RuntimeException("Track not found with ID: " + id));
  }

  @Override
  @Transactional(readOnly = true)
  public List<TrackResponse> findAllActive() {
    log.debug("Fetching all active tracks");
    return trackRepository.findAll().stream().filter(Track::getIsActive).map(TrackResponse::from).toList();
  }

  @Override
  @Transactional
  public TrackResponse create(CreateTrackRequest request) {
    log.debug("Creating track with request: {}", request);
    if (trackRepository.findByName(request.name()).isPresent()) {
      throw new RuntimeException("Track with name " + request.name() + " already exists");
    }
    Track track = new Track();
    track.setName(request.name());
    track.setDescription(request.description());
    track.setIsActive(true);
    trackRepository.save(track);
    return TrackResponse.from(track);
  }

  @Override
  @Transactional
  public TrackResponse update(Long id, UpdateTrackRequest request) {
    log.debug("Updating track with ID: {}", id);
    Track track = trackRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Track not found with ID: " + id));
    if (request.name() != null) track.setName(request.name());
    if (request.description() != null) track.setDescription(request.description());
    if (request.active() != null) track.setIsActive(request.active());
    trackRepository.save(track);
    return TrackResponse.from(track);
  }

  @Override
  @Transactional
  public void deactivate(Long id) {
    log.debug("Deactivating track with ID: {}", id);
    Track track = trackRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Track not found with ID: " + id));
    track.setIsActive(false);
    trackRepository.save(track);
  }

  @Override
  @Transactional
  public void activate(Long id) {
    log.debug("Activating track with ID: {}", id);
    Track track = trackRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Track not found with ID: " + id));
    track.setIsActive(true);
    trackRepository.save(track);
  }

  @Override
  @Transactional
  public void delete(Long id) {
    log.debug("Deleting track with ID: {}", id);
    Track track = trackRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Track not found with ID: " + id));
    if (track.getQuestions() != null && !track.getQuestions().isEmpty()) {
      throw new RuntimeException("Cannot delete track with existing questions");
    }
    if (track.getSessions() != null && !track.getSessions().isEmpty()) {
      throw new RuntimeException("Cannot delete track with existing interview sessions");
    }
    trackRepository.deleteById(id);
  }
}
