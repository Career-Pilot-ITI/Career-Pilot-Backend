package com.careerpilot.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportJobUrlRequest {

    @NotBlank(message = "url is required")
    @Pattern(regexp = "^(https?)://.+\\..+", message = "url must be a valid http(s) URL, e.g. https://www.linkedin.com/jobs/...")
    private String url;
}
