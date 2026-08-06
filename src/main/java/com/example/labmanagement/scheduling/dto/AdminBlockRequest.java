package com.example.labmanagement.scheduling.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;

public record AdminBlockRequest(String roomId, String deviceId, @NotNull LocalDate startDate,
		@NotNull LocalDate endDate, @Min(2) @Max(8) Integer dayOfWeek, List<@Positive Integer> periodIds,
		@NotBlank String reason) {
}
