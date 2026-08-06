package com.example.labmanagement.incident.dto;

import com.example.labmanagement.catalog.domain.LoaiTaiNguyen;
import com.example.labmanagement.incident.domain.MucDoSuCo;
import com.example.labmanagement.incident.domain.SuCoTrangThai;
import java.time.OffsetDateTime;

public record IncidentResponse(String id, long version, String resourceId, LoaiTaiNguyen resourceType,
		String resourceReferenceId, String resourceName, Long sessionId, String registrationId, String reporterId,
		String reporterName, String handlerId, String handlerName, MucDoSuCo severity, String description,
		SuCoTrangThai status, OffsetDateTime reportedAt, OffsetDateTime completedAt, String result) {
}
