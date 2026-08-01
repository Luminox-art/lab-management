package com.example.labmanagement.registration.application;

import com.example.labmanagement.registration.domain.LoaiPhieu;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record RegistrationSummaryResponse(String id, LoaiPhieu type, String purpose, String roomId, String roomName,
		int participantCount, LocalDate startDate, LocalDate endDate, PhieuDangKyTrangThai status, long version,
		String creatorId, String creatorName, OffsetDateTime createdAt, OffsetDateTime updatedAt,
		List<ApprovalWarningResponse> warnings) {

	public RegistrationSummaryResponse(String id, LoaiPhieu type, String purpose, String roomId, String roomName,
			int participantCount, LocalDate startDate, LocalDate endDate, PhieuDangKyTrangThai status, long version,
			String creatorId, String creatorName, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
		this(id, type, purpose, roomId, roomName, participantCount, startDate, endDate, status, version, creatorId,
				creatorName, createdAt, updatedAt, List.of());
	}
}
