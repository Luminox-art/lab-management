package com.example.labmanagement.reporting.dto;

import com.example.labmanagement.catalog.domain.LoaiTaiNguyen;
import com.example.labmanagement.maintenance.domain.BaoTriTrangThai;
import java.time.OffsetDateTime;

public record DashboardMaintenanceResponse(String maintenanceId, String resourceId, LoaiTaiNguyen resourceType,
		String resourceReferenceId, String resourceName, BaoTriTrangThai status, OffsetDateTime startAt) {
}
