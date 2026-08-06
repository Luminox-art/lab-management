package com.example.labmanagement.identity.dto;

import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import java.time.Instant;

public record UserProfileResponse(String id, String fullName, String email, String classOrUnit, String roleId,
		NguoiDungTrangThai status, long version, Instant createdAt, Instant updatedAt) {
}
