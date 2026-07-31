package com.example.labmanagement.catalog.application;

import com.example.labmanagement.catalog.domain.PhongTrangThai;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RoomCreateRequest(@NotBlank @Size(max = 50) String id, @NotBlank @Size(max = 150) String name,
		@NotBlank @Size(max = 50) String groupId, @NotBlank @Size(max = 255) String location, @Positive int capacity,
		@NotNull PhongTrangThai status) {
}
