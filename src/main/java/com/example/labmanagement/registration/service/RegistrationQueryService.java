package com.example.labmanagement.registration.service;

import com.example.labmanagement.catalog.domain.ThietBi;
import com.example.labmanagement.catalog.domain.ThietBiTrangThai;
import com.example.labmanagement.catalog.repository.ThietBiRepository;
import com.example.labmanagement.common.clock.TimeConfiguration;
import com.example.labmanagement.identity.domain.NguoiDung;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.repository.NguoiDungRepository;
import com.example.labmanagement.registration.domain.LichDangKy;
import com.example.labmanagement.registration.domain.LoaiPhieu;
import com.example.labmanagement.registration.domain.PhieuDangKy;
import com.example.labmanagement.registration.domain.PhieuDangKyThietBi;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import com.example.labmanagement.registration.domain.PhieuGiangDay;
import com.example.labmanagement.registration.domain.PhieuHuongDan;
import com.example.labmanagement.registration.domain.XuLyPhieu;
import com.example.labmanagement.registration.dto.RegistrationDeviceOptionResponse;
import com.example.labmanagement.registration.dto.RegistrationDeviceResponse;
import com.example.labmanagement.registration.dto.RegistrationHistoryResponse;
import com.example.labmanagement.registration.dto.RegistrationResponse;
import com.example.labmanagement.registration.dto.RegistrationScheduleResponse;
import com.example.labmanagement.registration.dto.RegistrationSummaryResponse;
import com.example.labmanagement.registration.dto.SupervisorOptionResponse;
import com.example.labmanagement.registration.repository.LichDangKyRepository;
import com.example.labmanagement.registration.repository.PhieuDangKyRepository;
import com.example.labmanagement.registration.repository.PhieuDangKyThietBiRepository;
import com.example.labmanagement.registration.repository.PhieuGiangDayRepository;
import com.example.labmanagement.registration.repository.PhieuHuongDanRepository;
import com.example.labmanagement.registration.repository.XuLyPhieuRepository;
import com.example.labmanagement.scheduling.domain.ScheduleDateCalculator;
import com.example.labmanagement.scheduling.domain.TietHoc;
import com.example.labmanagement.usage.domain.PhienSuDungTrangThai;
import com.example.labmanagement.usage.repository.PhienSuDungRepository;
import com.example.labmanagement.usage.repository.PhienSuDungThietBiRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RegistrationQueryService {

	private static final Set<ThietBiTrangThai> REQUESTABLE_DEVICE_STATUSES = Set.of(ThietBiTrangThai.SAN_SANG,
			ThietBiTrangThai.DANG_SU_DUNG);

	private final NguoiDungRepository userRepository;
	private final ThietBiRepository deviceRepository;
	private final PhieuDangKyRepository registrationRepository;
	private final LichDangKyRepository scheduleRepository;
	private final PhieuDangKyThietBiRepository allocationRepository;
	private final PhieuGiangDayRepository teachingRepository;
	private final PhieuHuongDanRepository supervisionRepository;
	private final XuLyPhieuRepository historyRepository;
	private final PhienSuDungRepository sessionRepository;
	private final PhienSuDungThietBiRepository sessionDeviceRepository;
	private final ApprovalService approvalService;
	private final RegistrationValidator validator;
	private final Clock clock;

	RegistrationQueryService(NguoiDungRepository userRepository, ThietBiRepository deviceRepository,
			PhieuDangKyRepository registrationRepository, LichDangKyRepository scheduleRepository,
			PhieuDangKyThietBiRepository allocationRepository, PhieuGiangDayRepository teachingRepository,
			PhieuHuongDanRepository supervisionRepository, XuLyPhieuRepository historyRepository,
			PhienSuDungRepository sessionRepository, PhienSuDungThietBiRepository sessionDeviceRepository,
			ApprovalService approvalService, RegistrationValidator validator, Clock clock) {
		this.userRepository = userRepository;
		this.deviceRepository = deviceRepository;
		this.registrationRepository = registrationRepository;
		this.scheduleRepository = scheduleRepository;
		this.allocationRepository = allocationRepository;
		this.teachingRepository = teachingRepository;
		this.supervisionRepository = supervisionRepository;
		this.historyRepository = historyRepository;
		this.sessionRepository = sessionRepository;
		this.sessionDeviceRepository = sessionDeviceRepository;
		this.approvalService = approvalService;
		this.validator = validator;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public Page<RegistrationSummaryResponse> search(String actorEmail, LoaiPhieu type, PhieuDangKyTrangThai status,
			String roomId, LocalDate date, String creator, int page, int size) {
		NguoiDung actor = validator.findActiveActor(actorEmail);
		Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
				Sort.by(Sort.Direction.DESC, "createdAt", "id"));
		String roleId = actor.getRole().getId();
		Page<PhieuDangKy> registrations;
		if (RegistrationValidator.ROLE_MANAGER.equals(roleId)) {
			registrations = registrationRepository.findQueue(type, PhieuDangKyTrangThai.CHO_DUYET,
					validator.normalizeOptional(roomId), date, validator.normalizeOptional(creator), pageable);
		} else if (RegistrationValidator.ROLE_INSTRUCTOR.equals(roleId)) {
			registrations = registrationRepository.findAccessibleToInstructor(actor.getId(), type, status, pageable);
		} else if (RegistrationValidator.ROLE_STUDENT.equals(roleId)) {
			registrations = registrationRepository.findOwnedByCreatorId(actor.getId(), type, status, pageable);
		} else {
			throw validator.accessDenied("Vai trò hiện tại không được truy cập phiếu đăng ký.");
		}
		return RegistrationValidator.ROLE_MANAGER.equals(roleId)
				? registrations.map(this::toManagerSummary)
				: registrations.map(this::toSummary);
	}

	@Transactional(readOnly = true)
	public RegistrationResponse get(String actorEmail, String registrationId) {
		NguoiDung actor = validator.findActiveActor(actorEmail);
		PhieuDangKy registration = validator.findRegistration(registrationId);
		validator.assertCanView(actor, registration);
		return toDetail(registration, actor);
	}

	@Transactional(readOnly = true)
	public List<SupervisorOptionResponse> supervisorOptions() {
		return userRepository
				.findAllByRole_IdAndStatusOrderByFullNameAsc(RegistrationValidator.ROLE_INSTRUCTOR,
						NguoiDungTrangThai.HOAT_DONG)
				.stream()
				.map(user -> new SupervisorOptionResponse(user.getId(), user.getFullName(), user.getClassOrUnit()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<RegistrationDeviceOptionResponse> deviceOptions() {
		List<ThietBi> devices = deviceRepository.findAllByStatusInOrderByNameAsc(REQUESTABLE_DEVICE_STATUSES);
		Map<String, String> usageRooms = devices.isEmpty()
				? Map.of()
				: sessionDeviceRepository
						.findActiveDeviceLocations(
								devices.stream().map(ThietBi::getId).toList(), PhienSuDungTrangThai.DANG_SU_DUNG)
						.stream()
						.collect(Collectors.toMap(PhienSuDungThietBiRepository.ActiveDeviceLocation::getDeviceId,
								PhienSuDungThietBiRepository.ActiveDeviceLocation::getUsageRoomId,
								(first, ignored) -> first));
		return devices.stream().map(device -> new RegistrationDeviceOptionResponse(device.getId(), device.getName(),
				device.getType().getName(), device.getType().isInstructorRequired(), device.getType().isMobile(),
				device.getRoom() == null ? null : device.getRoom().getId(), usageRooms.get(device.getId()))).toList();
	}

	RegistrationResponse toDetail(PhieuDangKy registration, NguoiDung actor) {
		PhieuGiangDay teaching = teachingRepository.findByRegistrationId(registration.getId()).orElse(null);
		PhieuHuongDan supervision = supervisionRepository.findByRegistrationId(registration.getId()).orElse(null);
		List<LichDangKy> schedules = scheduleRepository.findAllByRegistrationId(registration.getId());
		List<PhieuDangKyThietBi> devices = allocationRepository.findAllByRegistrationId(registration.getId());
		List<XuLyPhieu> history = historyRepository.findAllByRegistrationId(registration.getId());
		boolean owner = registration.getCreator().getId().equals(actor.getId());
		boolean canEdit = owner && registration.getStatus() == PhieuDangKyTrangThai.CHO_DUYET;
		boolean canCancel = owner
				&& (registration.getStatus() == PhieuDangKyTrangThai.CHO_DUYET
						|| registration.getStatus() == PhieuDangKyTrangThai.DA_DUYET)
				&& !sessionRepository.existsStartedByRegistrationId(registration.getId())
				&& isBeforeFirstSession(registration, schedules);
		return new RegistrationResponse(registration.getId(), registration.getType(), registration.getPurpose(),
				registration.getRoom().getId(), registration.getRoom().getName(), registration.getAttendeeCount(),
				registration.getStartDate(), registration.getEndDate(), registration.getStatus(),
				registration.getVersion(), registration.getCreator().getId(), registration.getCreator().getFullName(),
				teaching == null ? null : teaching.getCourseId(),
				teaching == null ? null : teaching.getClassGroupName(),
				supervision == null ? null : supervision.getInstructor().getId(),
				supervision == null ? null : supervision.getInstructor().getFullName(),
				schedules.stream().map(this::toScheduleResponse).toList(),
				devices.stream().map(this::toDeviceResponse).toList(),
				history.stream().map(this::toHistoryResponse).toList(), toDisplayTime(registration.getCreatedAt()),
				toDisplayTime(registration.getUpdatedAt()), canEdit, canCancel);
	}

	boolean isBeforeFirstSession(PhieuDangKy registration, List<LichDangKy> schedules) {
		LocalDateTime firstSession = schedules.stream().map(schedule -> {
			List<LocalDate> dates = ScheduleDateCalculator.datesForSystemDay(registration.getStartDate(),
					registration.getEndDate(), schedule.getDayOfWeek());
			return dates.isEmpty() ? null : LocalDateTime.of(dates.getFirst(), schedule.getPeriod().getStartTime());
		}).filter(java.util.Objects::nonNull).min(LocalDateTime::compareTo).orElse(null);
		LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), TimeConfiguration.DISPLAY_ZONE);
		return firstSession != null && now.isBefore(firstSession);
	}

	private RegistrationSummaryResponse toSummary(PhieuDangKy registration) {
		return new RegistrationSummaryResponse(registration.getId(), registration.getType(), registration.getPurpose(),
				registration.getRoom().getId(), registration.getRoom().getName(), registration.getAttendeeCount(),
				registration.getStartDate(), registration.getEndDate(), registration.getStatus(),
				registration.getVersion(), registration.getCreator().getId(), registration.getCreator().getFullName(),
				toDisplayTime(registration.getCreatedAt()), toDisplayTime(registration.getUpdatedAt()));
	}

	private RegistrationSummaryResponse toManagerSummary(PhieuDangKy registration) {
		RegistrationSummaryResponse summary = toSummary(registration);
		return new RegistrationSummaryResponse(summary.id(), summary.type(), summary.purpose(), summary.roomId(),
				summary.roomName(), summary.participantCount(), summary.startDate(), summary.endDate(),
				summary.status(), summary.version(), summary.creatorId(), summary.creatorName(), summary.createdAt(),
				summary.updatedAt(), approvalService.preview(registration).warnings());
	}

	private RegistrationScheduleResponse toScheduleResponse(LichDangKy schedule) {
		TietHoc period = schedule.getPeriod();
		return new RegistrationScheduleResponse(schedule.getDayOfWeek(),
				ScheduleDateCalculator.systemDayLabel(schedule.getDayOfWeek()), period.getId(), period.getName(),
				period.getStartTime(), period.getEndTime());
	}

	private RegistrationDeviceResponse toDeviceResponse(PhieuDangKyThietBi allocation) {
		ThietBi device = allocation.getDevice();
		return new RegistrationDeviceResponse(device.getId(), device.getName(), device.getType().getName(),
				device.getType().isInstructorRequired(), device.getType().isMobile(), allocation.isAllocated());
	}

	private RegistrationHistoryResponse toHistoryResponse(XuLyPhieu history) {
		return new RegistrationHistoryResponse(history.getAction(), history.getHandler().getId(),
				history.getHandler().getFullName(), history.getReason(), toDisplayTime(history.getOccurredAt()));
	}

	private OffsetDateTime toDisplayTime(Instant instant) {
		return instant == null ? null : instant.atZone(TimeConfiguration.DISPLAY_ZONE).toOffsetDateTime();
	}
}
