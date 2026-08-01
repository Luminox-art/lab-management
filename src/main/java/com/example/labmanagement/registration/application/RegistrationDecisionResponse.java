package com.example.labmanagement.registration.application;

import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import java.time.OffsetDateTime;
import java.util.List;

public record RegistrationDecisionResponse(String id, PhieuDangKyTrangThai status, long version,
		List<String> allocatedDeviceIds, OffsetDateTime processedAt) {
}
