package com.example.labmanagement.maintenance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MaintenanceCreateRequest(@NotBlank @Size(max = 50) String resourceId, @Size(max = 50) String incidentId,
		@NotBlank @Size(max = 50) String assigneeId, @NotBlank @Size(max = 2000) String content) {
}
