package com.example.labmanagement.registration.web;

import com.example.labmanagement.common.response.ApiResponse;
import com.example.labmanagement.registration.application.RegistrationCancelRequest;
import com.example.labmanagement.registration.application.RegistrationFormRequest;
import com.example.labmanagement.registration.application.RegistrationPageMeta;
import com.example.labmanagement.registration.application.RegistrationResponse;
import com.example.labmanagement.registration.application.RegistrationService;
import com.example.labmanagement.registration.application.RegistrationSummaryResponse;
import com.example.labmanagement.registration.domain.LoaiPhieu;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import jakarta.validation.Valid;
import java.security.Principal;
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

/** API-17..21, FR-10..14, UC-08..11. */
@RestController
@RequestMapping("/api/v1/registrations")
public class RegistrationRestController {

	private final RegistrationService registrationService;

	public RegistrationRestController(RegistrationService registrationService) {
		this.registrationService = registrationService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ApiResponse<RegistrationResponse> create(@Valid @RequestBody RegistrationFormRequest request, Principal principal) {
		return ApiResponse.of(registrationService.create(principal.getName(), request));
	}

	@GetMapping
	ApiResponse<List<RegistrationSummaryResponse>> registrations(@RequestParam(required = false) LoaiPhieu type,
			@RequestParam(required = false) PhieuDangKyTrangThai status, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size, Principal principal) {
		Page<RegistrationSummaryResponse> result = registrationService.search(principal.getName(), type, status, page,
				size);
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
}
