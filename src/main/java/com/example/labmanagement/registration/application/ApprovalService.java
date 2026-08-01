package com.example.labmanagement.registration.application;

import com.example.labmanagement.catalog.domain.Phong;
import com.example.labmanagement.catalog.domain.ThietBi;
import com.example.labmanagement.catalog.persistence.PhongRepository;
import com.example.labmanagement.catalog.persistence.TaiNguyenRepository;
import com.example.labmanagement.catalog.persistence.ThietBiRepository;
import com.example.labmanagement.common.clock.TimeConfiguration;
import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.common.error.ErrorCode;
import com.example.labmanagement.identity.domain.NguoiDung;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.persistence.NguoiDungRepository;
import com.example.labmanagement.registration.domain.HanhDongXuLyPhieu;
import com.example.labmanagement.registration.domain.LichDangKy;
import com.example.labmanagement.registration.domain.PhieuDangKy;
import com.example.labmanagement.registration.domain.PhieuDangKyThietBi;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import com.example.labmanagement.registration.domain.XuLyPhieu;
import com.example.labmanagement.registration.persistence.LichDangKyRepository;
import com.example.labmanagement.registration.persistence.PhieuDangKyRepository;
import com.example.labmanagement.registration.persistence.PhieuDangKyThietBiRepository;
import com.example.labmanagement.registration.persistence.PhieuHuongDanRepository;
import com.example.labmanagement.registration.persistence.XuLyPhieuRepository;
import com.example.labmanagement.scheduling.application.AvailabilityConflictResponse;
import com.example.labmanagement.scheduling.application.SchedulingService;
import com.example.labmanagement.usage.application.SessionGenerationService;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalService {

	private static final String ROLE_MANAGER = "CBQL";
	private static final String ROLE_STUDENT = "SV";
	private static final String EMPTY_DEVICE_LOCK_KEY = "__NO_SELECTED_DEVICE__";

	private final NguoiDungRepository userRepository;
	private final PhieuDangKyRepository registrationRepository;
	private final PhongRepository roomRepository;
	private final ThietBiRepository deviceRepository;
	private final TaiNguyenRepository resourceRepository;
	private final LichDangKyRepository scheduleRepository;
	private final PhieuDangKyThietBiRepository allocationRepository;
	private final PhieuHuongDanRepository supervisionRepository;
	private final XuLyPhieuRepository historyRepository;
	private final SchedulingService schedulingService;
	private final SessionGenerationService sessionGenerationService;
	private final EntityManager entityManager;
	private final Clock clock;

	public ApprovalService(NguoiDungRepository userRepository, PhieuDangKyRepository registrationRepository,
			PhongRepository roomRepository, ThietBiRepository deviceRepository, TaiNguyenRepository resourceRepository,
			LichDangKyRepository scheduleRepository, PhieuDangKyThietBiRepository allocationRepository,
			PhieuHuongDanRepository supervisionRepository, XuLyPhieuRepository historyRepository,
			SchedulingService schedulingService, SessionGenerationService sessionGenerationService,
			EntityManager entityManager, Clock clock) {
		this.userRepository = userRepository;
		this.registrationRepository = registrationRepository;
		this.roomRepository = roomRepository;
		this.deviceRepository = deviceRepository;
		this.resourceRepository = resourceRepository;
		this.scheduleRepository = scheduleRepository;
		this.allocationRepository = allocationRepository;
		this.supervisionRepository = supervisionRepository;
		this.historyRepository = historyRepository;
		this.schedulingService = schedulingService;
		this.sessionGenerationService = sessionGenerationService;
		this.entityManager = entityManager;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public ApprovalPreviewResponse preview(String actorEmail, String registrationId) {
		findManager(actorEmail);
		PhieuDangKy registration = findRegistration(registrationId);
		if (registration.getStatus() != PhieuDangKyTrangThai.CHO_DUYET) {
			return new ApprovalPreviewResponse(false, List.of(new ApprovalWarningResponse(ApprovalWarningType.CONFLICT,
					"Phiếu đã được xử lý bởi yêu cầu khác.")));
		}
		return preview(registration);
	}

	ApprovalPreviewResponse preview(PhieuDangKy registration) {
		List<PhieuDangKyThietBi> requested = allocationRepository.findAllByRegistrationId(registration.getId());
		return inspect(registration, requested.stream().map(PhieuDangKyThietBi::getDevice).toList());
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public RegistrationDecisionResponse approve(String actorEmail, String registrationId, ApprovalRequest request) {
		NguoiDung manager = findManager(actorEmail);
		if (request == null || request.version() == null) {
			throw validation("Version của phiếu không được để trống.");
		}
		PhieuDangKy registration = lockRegistration(registrationId);
		assertPendingAndVersion(registration, request.version());

		Phong room = roomRepository.findByIdForUpdate(registration.getRoom().getId())
				.orElseThrow(() -> notFound("Không tìm thấy phòng."));
		entityManager.refresh(room);
		List<String> requestedIds = allocationRepository.findRequestedDeviceIds(registration.getId());
		List<String> selectedIds = normalizeDeviceIds(request.deviceIds());
		if (!requestedIds.containsAll(selectedIds)) {
			throw validation("Chỉ được phân bổ thiết bị đã có trong danh sách yêu cầu của phiếu.");
		}

		List<ThietBi> selectedDevices = selectedIds.isEmpty()
				? List.of()
				: deviceRepository.findAllByIdForUpdateOrderById(selectedIds);
		if (selectedDevices.size() != selectedIds.size()) {
			throw notFound("Không tìm thấy một hoặc nhiều thiết bị.");
		}
		List<String> resourceDeviceIds = selectedIds.isEmpty() ? List.of(EMPTY_DEVICE_LOCK_KEY) : selectedIds;
		int lockedResourceCount = resourceRepository.lockForScheduling(room.getId(), resourceDeviceIds).size();
		if (lockedResourceCount != selectedDevices.size() + 1) {
			throw conflict("Tài nguyên phòng hoặc thiết bị chưa được cấu hình đầy đủ.");
		}

		validateFixedDeviceLocations(room, selectedDevices);
		ApprovalPreviewResponse validation = inspect(registration, selectedDevices);
		if (!validation.canApprove()) {
			throw decisionFailure(validation.warnings().getFirst());
		}

		List<PhieuDangKyThietBi> requested = allocationRepository.findAllByRegistrationId(registration.getId());
		for (PhieuDangKyThietBi allocation : requested) {
			allocation.setAllocated(selectedIds.contains(allocation.getDevice().getId()));
		}
		Instant processedAt = clock.instant();
		registration.approve(processedAt);
		historyRepository.save(new XuLyPhieu(registration, manager, HanhDongXuLyPhieu.PHE_DUYET, null, processedAt));
		registrationRepository.flush();
		entityManager.refresh(registration);
		sessionGenerationService.generateForRegistration(registration.getId());
		return decision(registration, selectedIds, processedAt);
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public RegistrationDecisionResponse reject(String actorEmail, String registrationId, RejectionRequest request) {
		NguoiDung manager = findManager(actorEmail);
		if (request == null || request.version() == null) {
			throw validation("Version của phiếu không được để trống.");
		}
		String reason = normalizeOptional(request.reason());
		if (reason == null) {
			throw validation("Lý do từ chối không được để trống.");
		}
		if (reason.length() > 255) {
			throw validation("Lý do từ chối không được vượt quá 255 ký tự.");
		}
		PhieuDangKy registration = lockRegistration(registrationId);
		assertPendingAndVersion(registration, request.version());
		Instant processedAt = clock.instant();
		registration.reject(processedAt);
		historyRepository.save(new XuLyPhieu(registration, manager, HanhDongXuLyPhieu.TU_CHOI, reason, processedAt));
		registrationRepository.flush();
		entityManager.refresh(registration);
		return decision(registration, List.of(), processedAt);
	}

	private ApprovalPreviewResponse inspect(PhieuDangKy registration, List<ThietBi> devices) {
		List<ApprovalWarningResponse> warnings = new ArrayList<>();
		if (registration.getAttendeeCount() > registration.getRoom().getCapacity()) {
			warnings.add(new ApprovalWarningResponse(ApprovalWarningType.CAPACITY,
					"Số người vượt quá sức chứa hiện tại của phòng."));
		}
		boolean controlledStudentRequest = ROLE_STUDENT.equals(registration.getCreator().getRole().getId())
				&& devices.stream().anyMatch(device -> device.getType().isInstructorRequired());
		if (controlledStudentRequest && supervisionRepository.findByRegistrationId(registration.getId()).isEmpty()) {
			warnings.add(new ApprovalWarningResponse(ApprovalWarningType.SUPERVISOR,
					"Phiếu sinh viên có thiết bị kiểm soát nhưng chưa có giảng viên hướng dẫn."));
		}
		devices.stream().filter(device -> !device.getType().isMobile()).filter(
				device -> device.getRoom() == null || !registration.getRoom().getId().equals(device.getRoom().getId()))
				.findFirst().ifPresent(device -> warnings.add(new ApprovalWarningResponse(ApprovalWarningType.CONFLICT,
						"Thiết bị cố định " + device.getId() + " không thuộc phòng đã chọn.")));
		List<AvailabilityConflictResponse> conflicts = availabilityConflicts(registration, devices);
		if (!conflicts.isEmpty()) {
			AvailabilityConflictResponse first = conflicts.getFirst();
			String suffix = conflicts.size() == 1 ? "" : " (và " + (conflicts.size() - 1) + " xung đột khác)";
			warnings.add(new ApprovalWarningResponse(ApprovalWarningType.CONFLICT, first.message() + suffix));
		}
		return new ApprovalPreviewResponse(warnings.isEmpty(), List.copyOf(warnings));
	}

	private List<AvailabilityConflictResponse> availabilityConflicts(PhieuDangKy registration, List<ThietBi> devices) {
		List<String> deviceIds = devices.stream().map(ThietBi::getId).sorted().toList();
		Map<String, AvailabilityConflictResponse> conflicts = new LinkedHashMap<>();
		for (LichDangKy schedule : scheduleRepository.findAllByRegistrationId(registration.getId())) {
			for (AvailabilityConflictResponse item : schedulingService
					.checkAvailability(registration.getRoom().getId(), deviceIds, registration.getStartDate(),
							registration.getEndDate(), schedule.getDayOfWeek(), schedule.getPeriod().getId())
					.conflicts()) {
				String key = item.type() + "|" + item.resourceId() + "|" + item.date() + "|" + item.periodId();
				conflicts.putIfAbsent(key, item);
			}
		}
		return List.copyOf(conflicts.values());
	}

	private void validateFixedDeviceLocations(Phong room, List<ThietBi> devices) {
		for (ThietBi device : devices) {
			if (!device.getType().isMobile()
					&& (device.getRoom() == null || !room.getId().equals(device.getRoom().getId()))) {
				throw conflict("Thiết bị cố định " + device.getId() + " không thuộc phòng đã chọn.");
			}
		}
	}

	private void assertPendingAndVersion(PhieuDangKy registration, long version) {
		if (registration.getStatus() != PhieuDangKyTrangThai.CHO_DUYET) {
			throw conflict("Phiếu đã được xử lý bởi yêu cầu khác.");
		}
		if (registration.getVersion() != version) {
			throw conflict("Phiếu đã được cập nhật bởi yêu cầu khác.");
		}
	}

	private NguoiDung findManager(String email) {
		String normalized = normalizeOptional(email);
		NguoiDung actor = normalized == null
				? null
				: userRepository.findByEmailIgnoreCase(normalized.toLowerCase(Locale.ROOT)).orElse(null);
		if (actor == null || actor.getStatus() != NguoiDungTrangThai.HOAT_DONG
				|| !ROLE_MANAGER.equals(actor.getRole().getId())) {
			throw accessDenied("Chỉ cán bộ quản lý đang hoạt động được xử lý phiếu.");
		}
		return actor;
	}

	private PhieuDangKy findRegistration(String id) {
		String normalized = normalizeOptional(id);
		return normalized == null
				? throwNotFound()
				: registrationRepository.findDetailById(normalized)
						.orElseThrow(() -> notFound("Không tìm thấy phiếu đăng ký."));
	}

	private PhieuDangKy lockRegistration(String id) {
		String normalized = normalizeOptional(id);
		return normalized == null
				? throwNotFound()
				: registrationRepository.findDetailByIdForUpdate(normalized)
						.orElseThrow(() -> notFound("Không tìm thấy phiếu đăng ký."));
	}

	private PhieuDangKy throwNotFound() {
		throw notFound("Không tìm thấy phiếu đăng ký.");
	}

	private List<String> normalizeDeviceIds(Collection<String> values) {
		if (values == null) {
			return List.of();
		}
		Map<String, String> unique = new LinkedHashMap<>();
		for (String value : values) {
			String normalized = normalizeOptional(value);
			if (normalized == null) {
				continue;
			}
			String key = normalized.toLowerCase(Locale.ROOT);
			if (unique.putIfAbsent(key, normalized) != null) {
				throw validation("Danh sách thiết bị phân bổ có mã bị trùng.");
			}
		}
		return unique.values().stream().sorted(Comparator.naturalOrder()).toList();
	}

	private RegistrationDecisionResponse decision(PhieuDangKy registration, List<String> allocatedDeviceIds,
			Instant processedAt) {
		OffsetDateTime displayTime = processedAt.atZone(TimeConfiguration.DISPLAY_ZONE).toOffsetDateTime();
		return new RegistrationDecisionResponse(registration.getId(), registration.getStatus(),
				registration.getVersion(), List.copyOf(allocatedDeviceIds), displayTime);
	}

	private ApiException decisionFailure(ApprovalWarningResponse warning) {
		HttpStatus status = warning.type() == ApprovalWarningType.CAPACITY
				|| warning.type() == ApprovalWarningType.SUPERVISOR
						? HttpStatus.UNPROCESSABLE_ENTITY
						: HttpStatus.CONFLICT;
		ErrorCode code = status == HttpStatus.CONFLICT
				? ErrorCode.RESOURCE_CONFLICT
				: ErrorCode.BUSINESS_RULE_VIOLATION;
		return new ApiException(code, status, warning.message());
	}

	private String normalizeOptional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private ApiException validation(String message) {
		return new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, message);
	}

	private ApiException accessDenied(String message) {
		return new ApiException(ErrorCode.ACCESS_DENIED, HttpStatus.FORBIDDEN, message);
	}

	private ApiException conflict(String message) {
		return new ApiException(ErrorCode.RESOURCE_CONFLICT, HttpStatus.CONFLICT, message);
	}

	private ApiException notFound(String message) {
		return new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, message);
	}
}
