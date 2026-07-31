package com.example.labmanagement.registration.application;

import com.example.labmanagement.registration.domain.LoaiPhieu;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record RegistrationResponse(String id, LoaiPhieu type, String purpose, String roomId, String roomName,
		int participantCount, LocalDate startDate, LocalDate endDate, PhieuDangKyTrangThai status, long version,
		String creatorId, String creatorName, String courseCode, String classGroup, String supervisorId,
		String supervisorName, List<RegistrationScheduleResponse> schedules, List<RegistrationDeviceResponse> devices,
		List<RegistrationHistoryResponse> history, OffsetDateTime createdAt, OffsetDateTime updatedAt, boolean canEdit,
		boolean canCancel) {
}
