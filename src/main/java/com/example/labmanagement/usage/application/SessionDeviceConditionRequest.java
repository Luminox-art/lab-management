package com.example.labmanagement.usage.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SessionDeviceConditionRequest(@NotBlank @Size(max = 50) String deviceId,
		@NotBlank @Size(max = 255) String condition, @Size(max = 2000) String note) {
}
