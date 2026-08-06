package com.example.labmanagement.scheduling.dto;

import java.time.LocalDate;
import java.util.List;

public record AvailabilityResponse(boolean available, LocalDate from, LocalDate to, int dayOfWeek,
		String dayOfWeekLabel, PeriodResponse period, List<LocalDate> requestedDates,
		List<AvailabilityConflictResponse> conflicts) {
}
