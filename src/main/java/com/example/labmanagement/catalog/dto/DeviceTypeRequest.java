package com.example.labmanagement.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceTypeRequest(@NotBlank @Size(max = 50) String id, @NotBlank @Size(max = 150) String name,
		boolean instructorRequired, boolean mobile, String description) {
}
