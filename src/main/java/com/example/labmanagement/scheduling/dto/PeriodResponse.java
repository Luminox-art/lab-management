package com.example.labmanagement.scheduling.dto;

import java.time.LocalTime;

public record PeriodResponse(int id, String name, LocalTime startTime, LocalTime endTime) {
}
