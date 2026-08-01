package com.example.labmanagement.incident.application;

import com.example.labmanagement.incident.domain.SuCoTrangThai;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IncidentUpdateRequest(@Size(max = 50) String handlerId, @NotNull SuCoTrangThai status,
		@Size(max = 2000) String result, @NotNull Long version) {
}
