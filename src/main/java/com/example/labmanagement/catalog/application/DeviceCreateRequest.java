package com.example.labmanagement.catalog.application;

import com.example.labmanagement.catalog.domain.ThietBiTrangThai;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeviceCreateRequest(@NotBlank @Size(max = 50) String id, @NotBlank @Size(max = 150) String name,
		@NotBlank @Size(max = 50) String typeId, @Size(max = 100) String serialNumber, @Size(max = 100) String model,
		@Size(max = 50) String roomId, @NotNull ThietBiTrangThai status) {
}
