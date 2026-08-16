package com.example.labmanagement.identity.dto;

import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AdminUserUpdateRequest(@NotBlank @Size(max = 150) String fullName,
		@NotBlank @Email @Size(max = 254) String email, @Size(max = 150) String classOrUnit,
		@NotBlank @Pattern(regexp = "ADMIN|CBQL|GV|SV") String roleId, @NotNull NguoiDungTrangThai status,
		@NotNull @PositiveOrZero Long version) {
}
