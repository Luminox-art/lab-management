package com.example.labmanagement.scheduling.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record CalendarEventResponse(CalendarEventType type, LocalDate date, int dayOfWeek, Integer periodId,
		String periodName, LocalTime startTime, LocalTime endTime, boolean allDay, String title, String detail) {
}
