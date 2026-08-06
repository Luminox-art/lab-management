package com.example.labmanagement.incident.dto;

import com.example.labmanagement.incident.domain.MucDoSuCo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record IncidentCreateRequest(@NotBlank @Size(max = 50) String resourceId, @Positive Long sessionId,
		@NotNull MucDoSuCo severity, @NotBlank @Size(max = 2000) String description) {
}
