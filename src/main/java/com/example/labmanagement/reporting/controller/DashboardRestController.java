package com.example.labmanagement.reporting.controller;

import com.example.labmanagement.common.response.ApiResponse;
import com.example.labmanagement.reporting.domain.DashboardGroup;
import com.example.labmanagement.reporting.dto.DashboardResponse;
import com.example.labmanagement.reporting.service.DashboardService;
import java.security.Principal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** API-34, FR-25..26, UC-22. */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardRestController {

	private final DashboardService dashboardService;

	public DashboardRestController(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	@GetMapping
	ApiResponse<DashboardResponse> dashboard(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) DashboardGroup groupBy, Principal principal) {
		return ApiResponse.of(dashboardService.dashboard(principal.getName(), from, to, groupBy));
	}
}
