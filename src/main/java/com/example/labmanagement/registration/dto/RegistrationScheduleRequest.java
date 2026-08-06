package com.example.labmanagement.registration.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record RegistrationScheduleRequest(@Min(2) @Max(8) int dayOfWeek, @Positive int periodId) {
}
