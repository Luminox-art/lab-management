package com.example.labmanagement.registration.dto;

import com.example.labmanagement.registration.domain.LoaiPhieu;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record RegistrationFormRequest(@NotNull LoaiPhieu type, @NotBlank @Size(max = 2000) String purpose,
		@NotBlank @Size(max = 50) String roomId, @NotNull @Positive Integer participantCount,
		@NotNull LocalDate startDate, @NotNull LocalDate endDate,
		@Size(min = 1, max = 128) List<@Valid RegistrationScheduleRequest> schedules,
		@Size(max = 500) List<@NotBlank @Size(max = 50) String> deviceIds, @Size(max = 50) String courseCode,
		@Size(max = 100) String classGroup, @Size(max = 50) String supervisorId, @PositiveOrZero Long version) {
}
