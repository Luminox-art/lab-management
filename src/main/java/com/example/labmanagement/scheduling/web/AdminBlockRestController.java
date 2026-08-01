package com.example.labmanagement.scheduling.web;

import com.example.labmanagement.common.response.ApiResponse;
import com.example.labmanagement.scheduling.application.AdminBlockCreationResponse;
import com.example.labmanagement.scheduling.application.AdminBlockRequest;
import com.example.labmanagement.scheduling.application.AdminBlockService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API-24/25, FR-17, UC-14. */
@RestController
@RequestMapping("/api/v1/admin-blocks")
public class AdminBlockRestController {

	private final AdminBlockService adminBlockService;

	public AdminBlockRestController(AdminBlockService adminBlockService) {
		this.adminBlockService = adminBlockService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ApiResponse<AdminBlockCreationResponse> create(@Valid @RequestBody AdminBlockRequest request, Principal principal) {
		return ApiResponse.of(adminBlockService.create(principal.getName(), request));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void cancel(@PathVariable long id, Principal principal) {
		adminBlockService.cancel(principal.getName(), id);
	}
}
