package com.example.labmanagement.usage.dto;

import com.example.labmanagement.usage.domain.PhienSuDungTrangThai;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

public record UsageSessionResponse(Long id, long version, String registrationId, LocalDate usageDate,
		PhienSuDungTrangThai status, String roomId, String roomName, String creatorId, String creatorName,
		Integer periodId, String periodName, LocalTime startTime, LocalTime endTime, OffsetDateTime checkedInAt,
		String checkedInBy, OffsetDateTime checkedOutAt, String checkedOutBy, List<SessionDeviceResponse> devices,
		List<String> incidentIds, boolean canCheckIn, boolean canCheckOut) {
}
