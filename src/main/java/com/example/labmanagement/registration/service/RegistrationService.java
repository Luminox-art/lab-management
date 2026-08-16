package com.example.labmanagement.registration.service;

import com.example.labmanagement.registration.domain.LoaiPhieu;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import com.example.labmanagement.registration.dto.RegistrationCancelRequest;
import com.example.labmanagement.registration.dto.RegistrationDeviceOptionResponse;
import com.example.labmanagement.registration.dto.RegistrationFormRequest;
import com.example.labmanagement.registration.dto.RegistrationResponse;
import com.example.labmanagement.registration.dto.RegistrationSummaryResponse;
import com.example.labmanagement.registration.dto.SupervisorOptionResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public class RegistrationService {

	private final RegistrationCommandService commandService;
	private final RegistrationQueryService queryService;

	public RegistrationService(RegistrationCommandService commandService, RegistrationQueryService queryService) {
		this.commandService = commandService;
		this.queryService = queryService;
	}

	public RegistrationResponse create(String actorEmail, RegistrationFormRequest request) {
		return commandService.create(actorEmail, request);
	}

	public Page<RegistrationSummaryResponse> search(String actorEmail, LoaiPhieu type, PhieuDangKyTrangThai status,
			int page, int size) {
		return queryService.search(actorEmail, type, status, null, null, null, page, size);
	}

	public Page<RegistrationSummaryResponse> search(String actorEmail, LoaiPhieu type, PhieuDangKyTrangThai status,
			String roomId, LocalDate date, String creator, int page, int size) {
		return queryService.search(actorEmail, type, status, roomId, date, creator, page, size);
	}

	public RegistrationResponse get(String actorEmail, String registrationId) {
		return queryService.get(actorEmail, registrationId);
	}

	public RegistrationResponse update(String actorEmail, String registrationId, RegistrationFormRequest request) {
		return commandService.update(actorEmail, registrationId, request);
	}

	public RegistrationResponse cancel(String actorEmail, String registrationId, RegistrationCancelRequest request) {
		return commandService.cancel(actorEmail, registrationId, request);
	}

	public List<SupervisorOptionResponse> supervisorOptions() {
		return queryService.supervisorOptions();
	}

	public List<RegistrationDeviceOptionResponse> deviceOptions() {
		return queryService.deviceOptions();
	}
}
