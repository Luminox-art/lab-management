package com.example.labmanagement.maintenance.dto;

import com.example.labmanagement.maintenance.domain.BaoTriTrangThai;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record MaintenanceUpdateRequest(@NotNull BaoTriTrangThai status,
		@NotBlank @Size(max = 2000) String progressContent, Instant endAt, @Size(max = 2000) String result,
		@NotNull Long version) {
}
