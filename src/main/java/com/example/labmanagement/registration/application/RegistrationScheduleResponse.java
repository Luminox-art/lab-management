package com.example.labmanagement.registration.application;

import java.time.LocalTime;

public record RegistrationScheduleResponse(int dayOfWeek, String dayOfWeekLabel, int periodId, String periodName,
		LocalTime startTime, LocalTime endTime) {
}
