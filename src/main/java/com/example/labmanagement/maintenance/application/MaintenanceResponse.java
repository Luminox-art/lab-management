package com.example.labmanagement.maintenance.application;

import com.example.labmanagement.catalog.domain.LoaiTaiNguyen;
import com.example.labmanagement.maintenance.domain.BaoTriTrangThai;
import java.time.OffsetDateTime;
import java.util.List;

public record MaintenanceResponse(String id, long version, String resourceId, LoaiTaiNguyen resourceType,
		String resourceReferenceId, String resourceName, String incidentId, String assigneeId, String assigneeName,
		OffsetDateTime startAt, OffsetDateTime endAt, String content, BaoTriTrangThai status, String result,
		List<MaintenanceProgressResponse> progress) {
}
