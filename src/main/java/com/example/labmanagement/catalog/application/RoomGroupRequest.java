package com.example.labmanagement.catalog.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoomGroupRequest(@NotBlank @Size(max = 50) String id, @NotBlank @Size(max = 150) String name,
		String description) {
}
