package com.careerpilot.backend.dto.request;

import com.careerpilot.backend.entity.ENUMs.JobWorkspaceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateWorkspaceStatusRequest {

  @NotNull(message = "status is required")
  private JobWorkspaceStatus status;
}
