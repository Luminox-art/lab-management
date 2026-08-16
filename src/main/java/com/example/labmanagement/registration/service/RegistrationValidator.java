package com.example.labmanagement.registration.service;

import com.example.labmanagement.catalog.domain.Phong;
import com.example.labmanagement.catalog.domain.ThietBi;
import com.example.labmanagement.catalog.domain.ThietBiTrangThai;
import com.example.labmanagement.catalog.repository.PhongRepository;
import com.example.labmanagement.catalog.repository.ThietBiRepository;
import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.common.error.ErrorCode;
import com.example.labmanagement.identity.domain.NguoiDung;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.repository.NguoiDungRepository;
import com.example.labmanagement.registration.domain.LoaiPhieu;
import com.example.labmanagement.registration.domain.PhieuDangKy;
import com.example.labmanagement.registration.dto.RegistrationFormRequest;
import com.example.labmanagement.registration.dto.RegistrationScheduleRequest;
import com.example.labmanagement.registration.repository.PhieuDangKyRepository;
import com.example.labmanagement.registration.repository.PhieuHuongDanRepository;
import com.example.labmanagement.scheduling.domain.ScheduleDateCalculator;
import com.example.labmanagement.scheduling.domain.TietHoc;
import com.example.labmanagement.scheduling.dto.AvailabilityConflictResponse;
import com.example.labmanagement.scheduling.dto.AvailabilityResponse;
import com.example.labmanagement.scheduling.repository.TietHocRepository;
import com.example.labmanagement.scheduling.service.SchedulingService;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
class RegistrationValidator {

	static final String ROLE_MANAGER = "CBQL";
	static final String ROLE_INSTRUCTOR = "GV";
	static final String ROLE_STUDENT = "SV";

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
	private final PhieuHuongDanRepository supervisionRepository;
	private final SchedulingService schedulingService;

	RegistrationValidator(NguoiDungRepository userRepository, PhongRepository roomRepository,
			ThietBiRepository deviceRepository, TietHocRepository periodRepository,
			PhieuDangKyRepository registrationRepository, PhieuHuongDanRepository supervisionRepository,
			SchedulingService schedulingService) {
		this.userRepository = userRepository;
		this.roomRepository = roomRepository;
		this.deviceRepository = deviceRepository;
		this.periodRepository = periodRepository;
		this.registrationRepository = registrationRepository;
		this.supervisionRepository = supervisionRepository;
		this.schedulingService = schedulingService;
	}

	PreparedRegistration prepare(NguoiDung actor, RegistrationFormRequest request) {
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

	NguoiDung findActiveActor(String email) {
		String normalizedEmail = normalizeOptional(email);
		NguoiDung actor = normalizedEmail == null
				? null
				: userRepository.findByEmailIgnoreCase(normalizedEmail.toLowerCase(Locale.ROOT)).orElse(null);
		if (actor == null || actor.getStatus() != NguoiDungTrangThai.HOAT_DONG) {
			throw accessDenied("Tài khoản hiện tại không còn quyền thực hiện thao tác này.");
		}
		return actor;
	}

	PhieuDangKy findRegistration(String id) {
		String normalizedId = normalizeOptional(id);
		if (normalizedId == null) {
			throw notFound("Không tìm thấy phiếu đăng ký.");
		}
		return registrationRepository.findDetailById(normalizedId)
				.orElseThrow(() -> notFound("Không tìm thấy phiếu đăng ký."));
	}

	void assertCanView(NguoiDung actor, PhieuDangKy registration) {
		if (ROLE_MANAGER.equals(actor.getRole().getId()) || registration.getCreator().getId().equals(actor.getId())) {
			return;
		}
		if (ROLE_INSTRUCTOR.equals(actor.getRole().getId())
				&& supervisionRepository.existsByRegistration_IdAndInstructor_Id(registration.getId(), actor.getId())) {
			return;
		}
		throw accessDenied("Bạn không có quyền xem phiếu đăng ký này.");
	}

	void assertOwner(NguoiDung actor, PhieuDangKy registration) {
		if (!registration.getCreator().getId().equals(actor.getId())) {
			throw accessDenied("Chỉ chủ phiếu được thực hiện thao tác này.");
		}
	}

	void assertVersion(PhieuDangKy registration, Long requestedVersion) {
		if (requestedVersion == null) {
			throw validation("Version của phiếu không được để trống.");
		}
		if (registration.getVersion() != requestedVersion) {
			throw conflict("Phiếu đã được cập nhật bởi yêu cầu khác.");
		}
	}

	String normalizeRequired(String value, String message) {
		String normalized = normalizeOptional(value);
		if (normalized == null) {
			throw validation(message);
		}
		return normalized;
	}

	String normalizeOptional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	ApiException validation(String message) {
		return new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, message);
	}

	ApiException business(String message) {
		return new ApiException(ErrorCode.BUSINESS_RULE_VIOLATION, HttpStatus.UNPROCESSABLE_ENTITY, message);
	}

	ApiException accessDenied(String message) {
		return new ApiException(ErrorCode.ACCESS_DENIED, HttpStatus.FORBIDDEN, message);
	}

	ApiException conflict(String message) {
		return new ApiException(ErrorCode.RESOURCE_CONFLICT, HttpStatus.CONFLICT, message);
	}

	ApiException notFound(String message) {
		return new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, message);
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
			throw conflict(first == null ? "Lịch đã chọn không khả dụng." : first.message());
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

	record PreparedSchedule(int dayOfWeek, TietHoc period) {
	}

	record TeachingData(String courseCode, String classGroup) {
	}

	record PreparedRegistration(Phong room, String purpose, int participantCount, List<PreparedSchedule> schedules,
			List<ThietBi> devices, TeachingData teaching, NguoiDung supervisor) {
	}
}
