package com.careerpilot.backend.service;

import com.careerpilot.backend.controller.response.TrackResponse;
import com.careerpilot.backend.dto.request.CreateTrackRequest;
import com.careerpilot.backend.dto.request.UpdateTrackRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ITrackService {
    Page<TrackResponse> findAll(Pageable pageable);

    TrackResponse findById(Long id);

    List<TrackResponse> findAllActive();

    TrackResponse create(CreateTrackRequest request);

    TrackResponse update(Long id, UpdateTrackRequest request);

    void deactivate(Long id);

    void activate(Long id);

    void delete(Long id);
}
