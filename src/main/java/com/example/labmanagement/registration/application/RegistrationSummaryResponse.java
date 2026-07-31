package com.example.labmanagement.registration.application;

import com.example.labmanagement.registration.domain.LoaiPhieu;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record RegistrationSummaryResponse(String id, LoaiPhieu type, String purpose, String roomId, String roomName,
		int participantCount, LocalDate startDate, LocalDate endDate, PhieuDangKyTrangThai status, long version,
		String creatorId, String creatorName, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}
