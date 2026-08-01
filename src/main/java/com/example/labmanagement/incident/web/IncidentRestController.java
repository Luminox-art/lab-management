package com.example.labmanagement.incident.web;

import com.example.labmanagement.common.response.ApiResponse;
import com.example.labmanagement.incident.application.IncidentCreateRequest;
import com.example.labmanagement.incident.application.IncidentPageMeta;
import com.example.labmanagement.incident.application.IncidentResponse;
import com.example.labmanagement.incident.application.IncidentService;
import com.example.labmanagement.incident.application.IncidentUpdateRequest;
import com.example.labmanagement.incident.domain.MucDoSuCo;
import com.example.labmanagement.incident.domain.SuCoTrangThai;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API-28..30, FR-20..21, UC-19..20. */
@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentRestController {

	private final IncidentService incidentService;

	public IncidentRestController(IncidentService incidentService) {
		this.incidentService = incidentService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ApiResponse<IncidentResponse> report(@Valid @RequestBody IncidentCreateRequest request, Principal principal) {
		return ApiResponse.of(incidentService.report(principal.getName(), request));
	}

	@GetMapping
	ApiResponse<List<IncidentResponse>> incidents(@RequestParam(required = false) SuCoTrangThai status,
			@RequestParam(required = false) MucDoSuCo severity, @RequestParam(required = false) String resourceId,
			@RequestParam(required = false) Long sessionId, @RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
			Principal principal) {
		Page<IncidentResponse> result = incidentService.search(principal.getName(), status, severity, resourceId,
				sessionId, keyword, page, size);
		return ApiResponse.of(result.getContent(), new IncidentPageMeta(result.getNumber(), result.getSize(),
				result.getTotalElements(), result.getTotalPages()));
	}

	@PatchMapping("/{id}")
	ApiResponse<IncidentResponse> update(@PathVariable String id, @Valid @RequestBody IncidentUpdateRequest request,
			Principal principal) {
		return ApiResponse.of(incidentService.update(principal.getName(), id, request));
	}
}
