package com.example.labmanagement.usage.application;

import com.example.labmanagement.incident.domain.MucDoSuCo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SessionIncidentRequest(@NotBlank @Size(max = 50) String deviceId, @NotNull MucDoSuCo severity,
		@NotBlank @Size(max = 2000) String description) {
}
