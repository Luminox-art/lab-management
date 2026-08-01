package com.example.labmanagement.maintenance.application;

import com.example.labmanagement.maintenance.domain.BaoTriTrangThai;
import java.time.OffsetDateTime;

public record MaintenanceProgressResponse(Long id, OffsetDateTime occurredAt, BaoTriTrangThai status, String content,
		String updatedById, String updatedByName) {
}
