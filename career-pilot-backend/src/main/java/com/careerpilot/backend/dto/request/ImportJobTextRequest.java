package com.careerpilot.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportJobTextRequest {

  @NotBlank(message = "descriptionText is required")
  private String descriptionText;

  private String sourceUrl;
}
