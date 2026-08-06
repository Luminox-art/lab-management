package com.example.labmanagement.usage.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SessionCheckInRequest(@NotNull @PositiveOrZero Long version,
		@NotNull @Size(max = 500) List<@Valid SessionDeviceConditionRequest> devices) {
}
