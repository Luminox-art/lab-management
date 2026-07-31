package com.example.labmanagement.identity.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record RegistrationRequest(@NotBlank @Size(max = 50) @Pattern(regexp = "[A-Za-z0-9._-]+") String identifier,
		@NotBlank @Size(max = 150) String fullName, @NotBlank @Email @Size(max = 254) String email,
		@NotBlank @Size(min = 8, max = 72) String password, @Size(max = 150) String organization,
		@NotNull RegistrationRole role) {

	public RegistrationRequest {
		identifier = trim(identifier);
		fullName = trim(fullName);
		email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
		organization = trim(organization);
	}

	private static String trim(String value) {
		return value == null ? null : value.trim();
	}
}
