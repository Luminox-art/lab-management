package com.example.labmanagement.identity.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(@NotBlank @Size(max = 150) String fullName,
		@NotBlank @Email @Size(max = 254) String email, @Size(max = 150) String classOrUnit) {
}
