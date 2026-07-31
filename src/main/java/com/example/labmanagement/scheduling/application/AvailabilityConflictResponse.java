package com.example.labmanagement.scheduling.application;

import com.example.labmanagement.catalog.domain.LoaiTaiNguyen;
import java.time.LocalDate;

public record AvailabilityConflictResponse(AvailabilityConflictType type, LoaiTaiNguyen resourceType, String resourceId,
		String resourceName, LocalDate date, int dayOfWeek, int periodId, String periodName, String message) {
}
