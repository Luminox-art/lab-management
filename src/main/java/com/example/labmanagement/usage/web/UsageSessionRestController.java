package com.example.labmanagement.usage.web;

import com.example.labmanagement.common.response.ApiResponse;
import com.example.labmanagement.usage.application.SessionCheckInRequest;
import com.example.labmanagement.usage.application.SessionCheckOutRequest;
import com.example.labmanagement.usage.application.UsageSessionResponse;
import com.example.labmanagement.usage.application.UsageSessionService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API-26..27, FR-18..19, UC-15..16. */
@RestController
@RequestMapping("/api/v1/sessions")
public class UsageSessionRestController {

	private final UsageSessionService sessionService;

	public UsageSessionRestController(UsageSessionService sessionService) {
		this.sessionService = sessionService;
	}

	@PostMapping("/{id}/check-in")
	ApiResponse<UsageSessionResponse> checkIn(@PathVariable Long id, @Valid @RequestBody SessionCheckInRequest request,
			Principal principal) {
		return ApiResponse.of(sessionService.checkIn(principal.getName(), id, request));
	}

	@PostMapping("/{id}/check-out")
	ApiResponse<UsageSessionResponse> checkOut(@PathVariable Long id,
			@Valid @RequestBody SessionCheckOutRequest request, Principal principal) {
		return ApiResponse.of(sessionService.checkOut(principal.getName(), id, request));
	}
}
