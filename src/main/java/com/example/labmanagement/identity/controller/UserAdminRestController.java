package com.example.labmanagement.identity.controller;

import com.example.labmanagement.common.response.ApiResponse;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.dto.AdminUserUpdateRequest;
import com.example.labmanagement.identity.dto.PageMeta;
import com.example.labmanagement.identity.dto.UserProfileResponse;
import com.example.labmanagement.identity.service.IdentityService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserAdminRestController {

	private final IdentityService identityService;

	public UserAdminRestController(IdentityService identityService) {
		this.identityService = identityService;
	}

	@GetMapping
	ApiResponse<List<UserProfileResponse>> users(@RequestParam(required = false) NguoiDungTrangThai status,
			@RequestParam(required = false) String role, @RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		Page<UserProfileResponse> result = identityService.searchUsers(status, role, keyword, page, size);
		return ApiResponse.of(result.getContent(),
				new PageMeta(result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages()));
	}

	@PatchMapping("/{id}")
	ApiResponse<UserProfileResponse> updateUser(@PathVariable String id,
			@Valid @RequestBody AdminUserUpdateRequest request) {
		return ApiResponse.of(identityService.updateUser(id, request));
	}
}
