package com.example.labmanagement.registration.service;

import com.example.labmanagement.catalog.domain.Phong;
import com.example.labmanagement.catalog.domain.ThietBi;
import com.example.labmanagement.catalog.domain.ThietBiTrangThai;
import com.example.labmanagement.catalog.repository.PhongRepository;
import com.example.labmanagement.catalog.repository.ThietBiRepository;
import com.example.labmanagement.common.clock.TimeConfiguration;
import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.common.error.ErrorCode;
import com.example.labmanagement.identity.domain.NguoiDung;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.repository.NguoiDungRepository;
import com.example.labmanagement.registration.domain.HanhDongXuLyPhieu;
import com.example.labmanagement.registration.domain.LichDangKy;
import com.example.labmanagement.registration.domain.LoaiPhieu;
import com.example.labmanagement.registration.domain.PhieuDangKy;
import com.example.labmanagement.registration.domain.PhieuDangKyThietBi;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import com.example.labmanagement.registration.domain.PhieuGiangDay;
import com.example.labmanagement.registration.domain.PhieuHuongDan;
import com.example.labmanagement.registration.domain.XuLyPhieu;
import com.example.labmanagement.registration.dto.RegistrationCancelRequest;
import com.example.labmanagement.registration.dto.RegistrationDeviceOptionResponse;
import com.example.labmanagement.registration.dto.RegistrationDeviceResponse;
import com.example.labmanagement.registration.dto.RegistrationFormRequest;
import com.example.labmanagement.registration.dto.RegistrationHistoryResponse;
import com.example.labmanagement.registration.dto.RegistrationResponse;
import com.example.labmanagement.registration.dto.RegistrationScheduleRequest;
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
import com.example.labmanagement.scheduling.dto.AvailabilityConflictResponse;
import com.example.labmanagement.scheduling.dto.AvailabilityResponse;
import com.example.labmanagement.scheduling.repository.TietHocRepository;
import com.example.labmanagement.scheduling.service.SchedulingService;
import com.example.labmanagement.usage.repository.PhienSuDungRepository;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

	private static final String ROLE_MANAGER = "CBQL";
	private static final String ROLE_INSTRUCTOR = "GV";
	private static final String ROLE_STUDENT = "SV";
	private static final int MAX_PURPOSE_LENGTH = 2000;
	private static final int MAX_SCHEDULES = 128;
	private static final int MAX_DEVICES = 500;
	private static final long MAX_REGISTRATION_DAYS = 3660;
	private static final LocalDate MYSQL_MIN_DATE = LocalDate.of(1000, 1, 1);
	private static final LocalDate MYSQL_MAX_DATE = LocalDate.of(9999, 12, 31);
	private static final Set<ThietBiTrangThai> REQUESTABLE_DEVICE_STATUSES = Set.of(ThietBiTrangThai.SAN_SANG,
			ThietBiTrangThai.DANG_SU_DUNG);

	private final NguoiDungRepository userRepository;
	private final PhongRepository roomRepository;
	private final ThietBiRepository deviceRepository;
	private final TietHocRepository periodRepository;
	private final PhieuDangKyRepository registrationRepository;
	private final LichDangKyRepository scheduleRepository;
	private final PhieuDangKyThietBiRepository allocationRepository;
	private final PhieuGiangDayRepository teachingRepository;
	private final PhieuHuongDanRepository supervisionRepository;
	private final XuLyPhieuRepository historyRepository;
	private final PhienSuDungRepository sessionRepository;
	private final ApprovalService approvalService;
	private final SchedulingService schedulingService;
	private final EntityManager entityManager;
	private final Clock clock;

	public RegistrationService(NguoiDungRepository userRepository, PhongRepository roomRepository,
			ThietBiRepository deviceRepository, TietHocRepository periodRepository,
			PhieuDangKyRepository registrationRepository, LichDangKyRepository scheduleRepository,
			PhieuDangKyThietBiRepository allocationRepository, PhieuGiangDayRepository teachingRepository,
			PhieuHuongDanRepository supervisionRepository, XuLyPhieuRepository historyRepository,
			PhienSuDungRepository sessionRepository, ApprovalService approvalService,
			SchedulingService schedulingService, EntityManager entityManager, Clock clock) {
		this.userRepository = userRepository;
		this.roomRepository = roomRepository;
		this.deviceRepository = deviceRepository;
		this.periodRepository = periodRepository;
		this.registrationRepository = registrationRepository;
		this.scheduleRepository = scheduleRepository;
		this.allocationRepository = allocationRepository;
		this.teachingRepository = teachingRepository;
		this.supervisionRepository = supervisionRepository;
		this.historyRepository = historyRepository;
		this.sessionRepository = sessionRepository;
		this.approvalService = approvalService;
		this.schedulingService = schedulingService;
		this.entityManager = entityManager;
		this.clock = clock;
	}

	@Transactional
	public RegistrationResponse create(String actorEmail, RegistrationFormRequest request) {
		NguoiDung actor = findActiveActor(actorEmail);
		PreparedRegistration prepared = prepare(actor, request);
		PhieuDangKy registration = new PhieuDangKy(newRegistrationId(), actor, prepared.room(), request.type(),
				prepared.purpose(), prepared.participantCount(), request.startDate(), request.endDate(),
				PhieuDangKyTrangThai.CHO_DUYET);
		registration = registrationRepository.save(registration);
		createChildren(registration, prepared);
		registrationRepository.flush();
		return toDetail(registration, actor);
	}

	@Transactional(readOnly = true)
	public Page<RegistrationSummaryResponse> search(String actorEmail, LoaiPhieu type, PhieuDangKyTrangThai status,
			int page, int size) {
		return search(actorEmail, type, status, null, null, null, page, size);
	}

	@Transactional(readOnly = true)
	public Page<RegistrationSummaryResponse> search(String actorEmail, LoaiPhieu type, PhieuDangKyTrangThai status,
			String roomId, LocalDate date, String creator, int page, int size) {
		NguoiDung actor = findActiveActor(actorEmail);
		Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
				Sort.by(Sort.Direction.DESC, "createdAt", "id"));
		String roleId = actor.getRole().getId();
		Page<PhieuDangKy> registrations;
		if (ROLE_MANAGER.equals(roleId)) {
			registrations = registrationRepository.findQueue(type, PhieuDangKyTrangThai.CHO_DUYET,
					normalizeOptional(roomId), date, normalizeOptional(creator), pageable);
		} else if (ROLE_INSTRUCTOR.equals(roleId)) {
			registrations = registrationRepository.findAccessibleToInstructor(actor.getId(), type, status, pageable);
		} else if (ROLE_STUDENT.equals(roleId)) {
			registrations = registrationRepository.findOwnedByCreatorId(actor.getId(), type, status, pageable);
		} else {
			throw accessDenied("Vai trò hiện tại không được truy cập phiếu đăng ký.");
		}
		return ROLE_MANAGER.equals(roleId)
				? registrations.map(this::toManagerSummary)
				: registrations.map(this::toSummary);
	}

	@Transactional(readOnly = true)
	public RegistrationResponse get(String actorEmail, String registrationId) {
		NguoiDung actor = findActiveActor(actorEmail);
		PhieuDangKy registration = findRegistration(registrationId);
		assertCanView(actor, registration);
		return toDetail(registration, actor);
	}

	@Transactional
	public RegistrationResponse update(String actorEmail, String registrationId, RegistrationFormRequest request) {
		NguoiDung actor = findActiveActor(actorEmail);
		PhieuDangKy registration = findRegistration(registrationId);
		assertOwner(actor, registration);
		if (registration.getStatus() != PhieuDangKyTrangThai.CHO_DUYET) {
			throw conflict("Chỉ được sửa phiếu đang chờ duyệt.");
		}
		assertVersion(registration, request.version());
		PreparedRegistration prepared = prepare(actor, request);
		registration.update(prepared.room(), request.type(), prepared.purpose(), prepared.participantCount(),
				request.startDate(), request.endDate(), clock.instant());
		deleteReplaceableChildren(registration.getId());
		entityManager.flush();
		createChildren(registration, prepared);
		entityManager.flush();
		entityManager.refresh(registration);
		return toDetail(registration, actor);
	}

	@Transactional
	public RegistrationResponse cancel(String actorEmail, String registrationId, RegistrationCancelRequest request) {
		NguoiDung actor = findActiveActor(actorEmail);
		PhieuDangKy registration = findRegistration(registrationId);
		assertOwner(actor, registration);
		assertVersion(registration, request.version());
		if (registration.getStatus() != PhieuDangKyTrangThai.CHO_DUYET
				&& registration.getStatus() != PhieuDangKyTrangThai.DA_DUYET) {
			throw conflict("Trạng thái hiện tại không cho phép hủy phiếu.");
		}
		List<LichDangKy> schedules = scheduleRepository.findAllByRegistrationId(registration.getId());
		if (sessionRepository.existsStartedByRegistrationId(registration.getId())
				|| !isBeforeFirstSession(registration, schedules)) {
			throw conflict("Không thể hủy phiếu sau khi phiên sử dụng đầu tiên đã bắt đầu.");
		}
		String reason = normalizeRequired(request.reason(), "Lý do hủy không được để trống.");
		registration.cancel();
		historyRepository.save(new XuLyPhieu(registration, actor, HanhDongXuLyPhieu.HUY, reason, clock.instant()));
		registrationRepository.flush();
		return toDetail(registration, actor);
	}

	@Transactional(readOnly = true)
	public List<SupervisorOptionResponse> supervisorOptions() {
		return userRepository.findAllByRole_IdAndStatusOrderByFullNameAsc(ROLE_INSTRUCTOR, NguoiDungTrangThai.HOAT_DONG)
				.stream()
				.map(user -> new SupervisorOptionResponse(user.getId(), user.getFullName(), user.getClassOrUnit()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<RegistrationDeviceOptionResponse> deviceOptions() {
		return deviceRepository.findAllByStatusInOrderByNameAsc(REQUESTABLE_DEVICE_STATUSES).stream()
				.map(device -> new RegistrationDeviceOptionResponse(device.getId(), device.getName(),
						device.getType().getName(), device.getType().isInstructorRequired(),
						device.getType().isMobile(), device.getRoom() == null ? null : device.getRoom().getId()))
				.toList();
	}

	private PreparedRegistration prepare(NguoiDung actor, RegistrationFormRequest request) {
		if (request == null || request.type() == null) {
			throw validation("Loại phiếu không được để trống.");
		}
		assertCanCreateType(actor, request.type());
		String purpose = normalizeRequired(request.purpose(), "Mục đích không được để trống.");
		if (purpose.length() > MAX_PURPOSE_LENGTH) {
			throw validation("Mục đích không được vượt quá 2.000 ký tự.");
		}
		Phong room = findRoom(request.roomId());
		int participantCount = request.participantCount() == null ? 0 : request.participantCount();
		if (participantCount <= 0) {
			throw validation("Số người phải lớn hơn 0.");
		}
		if (participantCount > room.getCapacity()) {
			throw business("Số người vượt quá sức chứa của phòng.");
		}
		validateDateRange(request.startDate(), request.endDate());
		List<PreparedSchedule> schedules = prepareSchedules(request.schedules(), request.startDate(),
				request.endDate());
		List<ThietBi> devices = prepareDevices(request.deviceIds(), room);
		TeachingData teaching = prepareTeaching(request.type(), request.courseCode(), request.classGroup());
		NguoiDung supervisor = prepareSupervisor(actor, devices, request.supervisorId());
		assertAvailable(room, request.startDate(), request.endDate(), schedules, devices);
		return new PreparedRegistration(room, purpose, participantCount, schedules, devices, teaching, supervisor);
	}

	private void assertAvailable(Phong room, LocalDate startDate, LocalDate endDate, List<PreparedSchedule> schedules,
			List<ThietBi> devices) {
		List<String> deviceIds = devices.stream().map(ThietBi::getId).sorted().toList();
		for (PreparedSchedule schedule : schedules) {
			AvailabilityResponse availability = schedulingService.checkAvailability(room.getId(), deviceIds, startDate,
					endDate, schedule.dayOfWeek(), schedule.period().getId());
			if (availability.available()) {
				continue;
			}
			AvailabilityConflictResponse first = availability.conflicts().isEmpty()
					? null
					: availability.conflicts().getFirst();
			String message = first == null ? "Lịch đã chọn không khả dụng." : first.message();
			throw conflict(message);
		}
	}

	private List<PreparedSchedule> prepareSchedules(List<RegistrationScheduleRequest> requestedSchedules,
			LocalDate startDate, LocalDate endDate) {
		if (requestedSchedules == null || requestedSchedules.isEmpty()) {
			throw validation("Phải có ít nhất một lịch sử dụng.");
		}
		if (requestedSchedules.size() > MAX_SCHEDULES) {
			throw validation("Một phiếu không được có quá 128 lịch sử dụng.");
		}
		Map<String, RegistrationScheduleRequest> uniqueSchedules = new LinkedHashMap<>();
		for (RegistrationScheduleRequest schedule : requestedSchedules) {
			if (schedule == null || schedule.dayOfWeek() < ScheduleDateCalculator.MONDAY
					|| schedule.dayOfWeek() > ScheduleDateCalculator.SUNDAY || schedule.periodId() <= 0) {
				throw validation("Thứ hoặc tiết học không hợp lệ.");
			}
			String key = schedule.dayOfWeek() + "|" + schedule.periodId();
			if (uniqueSchedules.putIfAbsent(key, schedule) != null) {
				throw validation("Danh sách lịch có thứ và tiết bị trùng.");
			}
			if (ScheduleDateCalculator.datesForSystemDay(startDate, endDate, schedule.dayOfWeek()).isEmpty()) {
				throw validation("Khoảng ngày không chứa ngày sử dụng tương ứng với lịch đã chọn.");
			}
		}
		Map<Integer, TietHoc> periods = new LinkedHashMap<>();
		periodRepository.findAllById(
				uniqueSchedules.values().stream().map(RegistrationScheduleRequest::periodId).distinct().toList())
				.forEach(period -> periods.put(period.getId(), period));
		if (periods.size() != uniqueSchedules.values().stream().map(RegistrationScheduleRequest::periodId).distinct()
				.count()) {
			throw notFound("Không tìm thấy một hoặc nhiều tiết học.");
		}
		return uniqueSchedules.values().stream()
				.map(schedule -> new PreparedSchedule(schedule.dayOfWeek(), periods.get(schedule.periodId())))
				.sorted(Comparator.comparingInt(PreparedSchedule::dayOfWeek)
						.thenComparing(prepared -> prepared.period().getId()))
				.toList();
	}

	private List<ThietBi> prepareDevices(Collection<String> requestedDeviceIds, Phong room) {
		if (requestedDeviceIds != null && requestedDeviceIds.size() > MAX_DEVICES) {
			throw validation("Một phiếu không được yêu cầu quá 500 thiết bị.");
		}
		List<String> deviceIds = normalizeUniqueIds(requestedDeviceIds, "Danh sách thiết bị có mã bị trùng.");
		if (deviceIds.isEmpty()) {
			return List.of();
		}
		List<ThietBi> devices = deviceRepository.findAllById(deviceIds);
		if (devices.size() != deviceIds.size()) {
			throw notFound("Không tìm thấy một hoặc nhiều thiết bị.");
		}
		for (ThietBi device : devices) {
			if (!REQUESTABLE_DEVICE_STATUSES.contains(device.getStatus())) {
				throw business("Thiết bị " + device.getId() + " không ở trạng thái cho phép đăng ký.");
			}
			if (!device.getType().isMobile()
					&& (device.getRoom() == null || !room.getId().equals(device.getRoom().getId()))) {
				throw business("Thiết bị cố định " + device.getId() + " không thuộc phòng đã chọn.");
			}
		}
		return devices.stream().sorted(Comparator.comparing(ThietBi::getId)).toList();
	}

	private TeachingData prepareTeaching(LoaiPhieu type, String courseCode, String classGroup) {
		String normalizedCourse = normalizeOptional(courseCode);
		String normalizedClassGroup = normalizeOptional(classGroup);
		if (type == LoaiPhieu.GIANG_DAY) {
			if (normalizedCourse == null || normalizedClassGroup == null) {
				throw business("Phiếu giảng dạy bắt buộc có mã học phần và lớp/nhóm.");
			}
			return new TeachingData(normalizedCourse, normalizedClassGroup);
		}
		if (normalizedCourse != null || normalizedClassGroup != null) {
			throw business("Mã học phần và lớp/nhóm chỉ áp dụng cho phiếu giảng dạy.");
		}
		return null;
	}

	private NguoiDung prepareSupervisor(NguoiDung actor, List<ThietBi> devices, String supervisorId) {
		String normalizedSupervisorId = normalizeOptional(supervisorId);
		boolean student = ROLE_STUDENT.equals(actor.getRole().getId());
		boolean required = student && devices.stream().anyMatch(device -> device.getType().isInstructorRequired());
		if (!required) {
			if (normalizedSupervisorId != null) {
				throw business("Chỉ chọn giảng viên hướng dẫn khi sinh viên đăng ký thiết bị có kiểm soát.");
			}
			return null;
		}
		if (normalizedSupervisorId == null) {
			throw business("Phiếu sinh viên có thiết bị kiểm soát bắt buộc phải chọn giảng viên hướng dẫn.");
		}
		NguoiDung supervisor = userRepository.findById(normalizedSupervisorId)
				.orElseThrow(() -> notFound("Không tìm thấy giảng viên hướng dẫn."));
		if (!ROLE_INSTRUCTOR.equals(supervisor.getRole().getId())
				|| supervisor.getStatus() != NguoiDungTrangThai.HOAT_DONG) {
			throw business("Giảng viên hướng dẫn phải là tài khoản giảng viên đang hoạt động.");
		}
		return supervisor;
	}

	private void assertCanCreateType(NguoiDung actor, LoaiPhieu type) {
		String roleId = actor.getRole().getId();
		if (ROLE_MANAGER.equals(roleId) || (!ROLE_INSTRUCTOR.equals(roleId) && !ROLE_STUDENT.equals(roleId))) {
			throw accessDenied("Vai trò hiện tại không được tạo phiếu đăng ký.");
		}
		if (type == LoaiPhieu.GIANG_DAY && !ROLE_INSTRUCTOR.equals(roleId)) {
			throw accessDenied("Chỉ giảng viên được tạo phiếu giảng dạy.");
		}
	}

	private void createChildren(PhieuDangKy registration, PreparedRegistration prepared) {
		if (prepared.teaching() != null) {
			entityManager.persist(new PhieuGiangDay(registration, prepared.teaching().courseCode(),
					prepared.teaching().classGroup()));
		}
		if (prepared.supervisor() != null) {
			entityManager.persist(new PhieuHuongDan(registration, prepared.supervisor()));
		}
		scheduleRepository.saveAll(prepared.schedules().stream()
				.map(schedule -> new LichDangKy(registration, schedule.dayOfWeek(), schedule.period())).toList());
		for (ThietBi device : prepared.devices()) {
			entityManager.persist(new PhieuDangKyThietBi(registration, device, false));
		}
	}

	private void deleteReplaceableChildren(String registrationId) {
		teachingRepository.findByRegistrationId(registrationId).ifPresent(entityManager::remove);
		supervisionRepository.findByRegistrationId(registrationId).ifPresent(entityManager::remove);
		allocationRepository.findAllByRegistrationId(registrationId).forEach(entityManager::remove);
		scheduleRepository.findAllByRegistrationId(registrationId).forEach(entityManager::remove);
	}

	private RegistrationResponse toDetail(PhieuDangKy registration, NguoiDung actor) {
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

	private void assertCanView(NguoiDung actor, PhieuDangKy registration) {
		if (ROLE_MANAGER.equals(actor.getRole().getId()) || registration.getCreator().getId().equals(actor.getId())) {
			return;
		}
		if (ROLE_INSTRUCTOR.equals(actor.getRole().getId())
				&& supervisionRepository.existsByRegistration_IdAndInstructor_Id(registration.getId(), actor.getId())) {
			return;
		}
		throw accessDenied("Bạn không có quyền xem phiếu đăng ký này.");
	}

	private void assertOwner(NguoiDung actor, PhieuDangKy registration) {
		if (!registration.getCreator().getId().equals(actor.getId())) {
			throw accessDenied("Chỉ chủ phiếu được thực hiện thao tác này.");
		}
	}

	private void assertVersion(PhieuDangKy registration, Long requestedVersion) {
		if (requestedVersion == null) {
			throw validation("Version của phiếu không được để trống.");
		}
		if (registration.getVersion() != requestedVersion) {
			throw conflict("Phiếu đã được cập nhật bởi yêu cầu khác.");
		}
	}

	private boolean isBeforeFirstSession(PhieuDangKy registration, List<LichDangKy> schedules) {
		LocalDateTime firstSession = schedules.stream().map(schedule -> {
			List<LocalDate> dates = ScheduleDateCalculator.datesForSystemDay(registration.getStartDate(),
					registration.getEndDate(), schedule.getDayOfWeek());
			return dates.isEmpty() ? null : LocalDateTime.of(dates.getFirst(), schedule.getPeriod().getStartTime());
		}).filter(java.util.Objects::nonNull).min(LocalDateTime::compareTo).orElse(null);
		LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), TimeConfiguration.DISPLAY_ZONE);
		return firstSession != null && now.isBefore(firstSession);
	}

	private NguoiDung findActiveActor(String email) {
		String normalizedEmail = normalizeOptional(email);
		NguoiDung actor = normalizedEmail == null
				? null
				: userRepository.findByEmailIgnoreCase(normalizedEmail.toLowerCase(Locale.ROOT)).orElse(null);
		if (actor == null || actor.getStatus() != NguoiDungTrangThai.HOAT_DONG) {
			throw accessDenied("Tài khoản hiện tại không còn quyền thực hiện thao tác này.");
		}
		return actor;
	}

	private PhieuDangKy findRegistration(String id) {
		String normalizedId = normalizeOptional(id);
		if (normalizedId == null) {
			throw notFound("Không tìm thấy phiếu đăng ký.");
		}
		return registrationRepository.findDetailById(normalizedId)
				.orElseThrow(() -> notFound("Không tìm thấy phiếu đăng ký."));
	}

	private Phong findRoom(String roomId) {
		String normalizedId = normalizeOptional(roomId);
		if (normalizedId == null) {
			throw validation("Phòng không được để trống.");
		}
		return roomRepository.findById(normalizedId).orElseThrow(() -> notFound("Không tìm thấy phòng."));
	}

	private void validateDateRange(LocalDate startDate, LocalDate endDate) {
		try {
			ScheduleDateCalculator.validateRange(startDate, endDate);
		} catch (IllegalArgumentException exception) {
			throw validation(exception.getMessage());
		}
		if (startDate.isBefore(MYSQL_MIN_DATE) || endDate.isAfter(MYSQL_MAX_DATE)) {
			throw validation("Khoảng ngày phải nằm trong miền ngày được MySQL hỗ trợ.");
		}
		if (ChronoUnit.DAYS.between(startDate, endDate) + 1 > MAX_REGISTRATION_DAYS) {
			throw validation("Khoảng đăng ký không được vượt quá 3.660 ngày.");
		}
	}

	private List<String> normalizeUniqueIds(Collection<String> values, String duplicateMessage) {
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
				throw validation(duplicateMessage);
			}
		}
		return new ArrayList<>(unique.values());
	}

	private String normalizeRequired(String value, String message) {
		String normalized = normalizeOptional(value);
		if (normalized == null) {
			throw validation(message);
		}
		return normalized;
	}

	private String normalizeOptional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private String newRegistrationId() {
		return "PDK-" + UUID.randomUUID();
	}

	private OffsetDateTime toDisplayTime(Instant instant) {
		return instant == null ? null : instant.atZone(TimeConfiguration.DISPLAY_ZONE).toOffsetDateTime();
	}

	private ApiException validation(String message) {
		return new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, message);
	}

	private ApiException business(String message) {
		return new ApiException(ErrorCode.BUSINESS_RULE_VIOLATION, HttpStatus.UNPROCESSABLE_ENTITY, message);
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

	private record PreparedSchedule(int dayOfWeek, TietHoc period) {
	}

	private record TeachingData(String courseCode, String classGroup) {
	}

	private record PreparedRegistration(Phong room, String purpose, int participantCount,
			List<PreparedSchedule> schedules, List<ThietBi> devices, TeachingData teaching, NguoiDung supervisor) {
	}
}
