package com.example.labmanagement.identity.controller;

import com.example.labmanagement.common.response.ApiResponse;
import com.example.labmanagement.identity.dto.PasswordChangeRequest;
import com.example.labmanagement.identity.dto.ProfileUpdateRequest;
import com.example.labmanagement.identity.dto.UserProfileResponse;
import com.example.labmanagement.identity.service.IdentityService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class ProfileRestController {

	private final IdentityService identityService;

	public ProfileRestController(IdentityService identityService) {
		this.identityService = identityService;
	}

	@GetMapping
	ApiResponse<UserProfileResponse> profile(Authentication authentication) {
		return ApiResponse.of(identityService.getProfile(authentication.getName()));
	}

	@PatchMapping
	ApiResponse<UserProfileResponse> updateProfile(Authentication authentication,
			@Valid @RequestBody ProfileUpdateRequest request) {
		return ApiResponse.of(identityService.updateProfile(authentication.getName(), request));
	}

	@PutMapping("/password")
	ResponseEntity<Void> changePassword(Authentication authentication,
			@Valid @RequestBody PasswordChangeRequest request) {
		identityService.changePassword(authentication.getName(), request);
		return ResponseEntity.noContent().build();
	}
}
