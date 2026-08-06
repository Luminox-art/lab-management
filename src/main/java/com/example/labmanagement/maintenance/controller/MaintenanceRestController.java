package com.example.labmanagement.maintenance.controller;

import com.example.labmanagement.common.response.ApiResponse;
import com.example.labmanagement.maintenance.dto.MaintenanceCreateRequest;
import com.example.labmanagement.maintenance.dto.MaintenanceResponse;
import com.example.labmanagement.maintenance.dto.MaintenanceUpdateRequest;
import com.example.labmanagement.maintenance.service.MaintenanceService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API-31..32, FR-22..23, UC-21..22. */
@RestController
@RequestMapping("/api/v1/maintenances")
public class MaintenanceRestController {

	private final MaintenanceService maintenanceService;

	public MaintenanceRestController(MaintenanceService maintenanceService) {
		this.maintenanceService = maintenanceService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ApiResponse<MaintenanceResponse> create(@Valid @RequestBody MaintenanceCreateRequest request, Principal principal) {
		return ApiResponse.of(maintenanceService.create(principal.getName(), request));
	}

	@PatchMapping("/{id}")
	ApiResponse<MaintenanceResponse> update(@PathVariable String id,
			@Valid @RequestBody MaintenanceUpdateRequest request, Principal principal) {
		return ApiResponse.of(maintenanceService.update(principal.getName(), id, request));
	}
}
