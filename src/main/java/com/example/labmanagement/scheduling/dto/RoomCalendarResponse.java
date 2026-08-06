package com.example.labmanagement.scheduling.dto;

import java.time.LocalDate;
import java.util.List;

public record RoomCalendarResponse(String roomId, String roomName, LocalDate from, LocalDate to,
		List<PeriodResponse> periods, List<CalendarEventResponse> events) {
}
