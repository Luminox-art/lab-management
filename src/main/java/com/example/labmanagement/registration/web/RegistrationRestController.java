package com.example.labmanagement.registration.web;

import com.example.labmanagement.common.response.ApiResponse;
import com.example.labmanagement.registration.application.ApprovalRequest;
import com.example.labmanagement.registration.application.ApprovalService;
import com.example.labmanagement.registration.application.RegistrationCancelRequest;
import com.example.labmanagement.registration.application.RegistrationDecisionResponse;
import com.example.labmanagement.registration.application.RegistrationFormRequest;
import com.example.labmanagement.registration.application.RegistrationPageMeta;
import com.example.labmanagement.registration.application.RegistrationResponse;
import com.example.labmanagement.registration.application.RegistrationService;
import com.example.labmanagement.registration.application.RegistrationSummaryResponse;
import com.example.labmanagement.registration.application.RejectionRequest;
import com.example.labmanagement.registration.domain.LoaiPhieu;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API-17..23, FR-10..16, UC-08..13. */
@RestController
@RequestMapping("/api/v1/registrations")
public class RegistrationRestController {

	private final RegistrationService registrationService;
	private final ApprovalService approvalService;

	public RegistrationRestController(RegistrationService registrationService, ApprovalService approvalService) {
		this.registrationService = registrationService;
		this.approvalService = approvalService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ApiResponse<RegistrationResponse> create(@Valid @RequestBody RegistrationFormRequest request, Principal principal) {
		return ApiResponse.of(registrationService.create(principal.getName(), request));
	}

	@GetMapping
	ApiResponse<List<RegistrationSummaryResponse>> registrations(@RequestParam(required = false) LoaiPhieu type,
			@RequestParam(required = false) PhieuDangKyTrangThai status, @RequestParam(required = false) String roomId,
			@RequestParam(required = false) LocalDate date, @RequestParam(required = false) String creator,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
			Principal principal) {
		Page<RegistrationSummaryResponse> result = registrationService.search(principal.getName(), type, status, roomId,
				date, creator, page, size);
		return ApiResponse.of(result.getContent(), new RegistrationPageMeta(result.getNumber(), result.getSize(),
				result.getTotalElements(), result.getTotalPages()));
	}

	@GetMapping("/{id}")
	ApiResponse<RegistrationResponse> registration(@PathVariable String id, Principal principal) {
		return ApiResponse.of(registrationService.get(principal.getName(), id));
	}

	@PutMapping("/{id}")
	ApiResponse<RegistrationResponse> update(@PathVariable String id,
			@Valid @RequestBody RegistrationFormRequest request, Principal principal) {
		return ApiResponse.of(registrationService.update(principal.getName(), id, request));
	}

	@PostMapping("/{id}/cancel")
	ApiResponse<RegistrationResponse> cancel(@PathVariable String id,
			@Valid @RequestBody RegistrationCancelRequest request, Principal principal) {
		return ApiResponse.of(registrationService.cancel(principal.getName(), id, request));
	}

	@PostMapping("/{id}/approve")
	ApiResponse<RegistrationDecisionResponse> approve(@PathVariable String id,
			@Valid @RequestBody ApprovalRequest request, Principal principal) {
		return ApiResponse.of(approvalService.approve(principal.getName(), id, request));
	}

	@PostMapping("/{id}/reject")
	ApiResponse<RegistrationDecisionResponse> reject(@PathVariable String id,
			@Valid @RequestBody RejectionRequest request, Principal principal) {
		return ApiResponse.of(approvalService.reject(principal.getName(), id, request));
	}
}
