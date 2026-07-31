package com.example.labmanagement.scheduling.application;

import com.example.labmanagement.catalog.domain.LoaiTaiNguyen;
import com.example.labmanagement.catalog.domain.Phong;
import com.example.labmanagement.catalog.domain.PhongTrangThai;
import com.example.labmanagement.catalog.domain.TaiNguyen;
import com.example.labmanagement.catalog.domain.ThietBi;
import com.example.labmanagement.catalog.domain.ThietBiTrangThai;
import com.example.labmanagement.catalog.persistence.PhongRepository;
import com.example.labmanagement.catalog.persistence.TaiNguyenRepository;
import com.example.labmanagement.catalog.persistence.ThietBiRepository;
import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.common.error.ErrorCode;
import com.example.labmanagement.registration.domain.LichDangKy;
import com.example.labmanagement.registration.domain.PhieuDangKy;
import com.example.labmanagement.registration.domain.PhieuDangKyThietBi;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import com.example.labmanagement.registration.persistence.LichDangKyRepository;
import com.example.labmanagement.registration.persistence.PhieuDangKyThietBiRepository;
import com.example.labmanagement.scheduling.domain.LichChan;
import com.example.labmanagement.scheduling.domain.LichChanTrangThai;
import com.example.labmanagement.scheduling.domain.ScheduleDateCalculator;
import com.example.labmanagement.scheduling.domain.TietHoc;
import com.example.labmanagement.scheduling.persistence.LichChanRepository;
import com.example.labmanagement.scheduling.persistence.TietHocRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchedulingService {

	private static final Set<PhieuDangKyTrangThai> OCCUPYING_STATUSES = Set.of(PhieuDangKyTrangThai.DA_DUYET,
			PhieuDangKyTrangThai.DANG_SU_DUNG);

	private final PhongRepository roomRepository;
	private final ThietBiRepository deviceRepository;
	private final TaiNguyenRepository resourceRepository;
	private final TietHocRepository periodRepository;
	private final LichDangKyRepository scheduleRepository;
	private final PhieuDangKyThietBiRepository allocationRepository;
	private final LichChanRepository blockedScheduleRepository;

	public SchedulingService(PhongRepository roomRepository, ThietBiRepository deviceRepository,
			TaiNguyenRepository resourceRepository, TietHocRepository periodRepository,
			LichDangKyRepository scheduleRepository, PhieuDangKyThietBiRepository allocationRepository,
			LichChanRepository blockedScheduleRepository) {
		this.roomRepository = roomRepository;
		this.deviceRepository = deviceRepository;
		this.resourceRepository = resourceRepository;
		this.periodRepository = periodRepository;
		this.scheduleRepository = scheduleRepository;
		this.allocationRepository = allocationRepository;
		this.blockedScheduleRepository = blockedScheduleRepository;
	}

	@Transactional(readOnly = true)
	public AvailabilityResponse checkAvailability(String roomId, Collection<String> deviceIds, LocalDate from,
			LocalDate to, int dayOfWeek, int periodId) {
		validateDateQuery(from, to, dayOfWeek);
		String normalizedRoomId = normalizeOptional(roomId);
		List<String> normalizedDeviceIds = normalizeDeviceIds(deviceIds);
		if (normalizedRoomId == null && normalizedDeviceIds.isEmpty()) {
			throw validation("Phải chọn ít nhất một phòng hoặc thiết bị.");
		}

		TietHoc period = findPeriod(periodId);
		List<LocalDate> requestedDates = ScheduleDateCalculator.datesForSystemDay(from, to, dayOfWeek);
		LinkedHashMap<String, AvailabilityConflictResponse> conflicts = new LinkedHashMap<>();
		Phong room = normalizedRoomId == null ? null : findRoom(normalizedRoomId);
		List<ThietBi> devices = findDevices(normalizedDeviceIds);

		addResourceStatusConflicts(room, devices, dayOfWeek, period, conflicts);
		addRoomRegistrationConflicts(room, from, to, dayOfWeek, period, conflicts);
		addDeviceRegistrationConflicts(devices, from, to, dayOfWeek, period, conflicts);
		addBlockedScheduleConflicts(room, devices, from, to, dayOfWeek, period, conflicts);

		List<AvailabilityConflictResponse> result = conflicts.values().stream().sorted(Comparator.comparing(
				(AvailabilityConflictResponse conflict) -> conflict.date() == null ? LocalDate.MIN : conflict.date())
				.thenComparing(conflict -> conflict.resourceType().name())
				.thenComparing(AvailabilityConflictResponse::resourceId)
				.thenComparing(conflict -> conflict.type().name())).toList();
		return new AvailabilityResponse(result.isEmpty(), from, to, dayOfWeek,
				ScheduleDateCalculator.systemDayLabel(dayOfWeek), toPeriodResponse(period), requestedDates, result);
	}

	@Transactional(readOnly = true)
	public RoomCalendarResponse roomCalendar(String roomId, LocalDate from, LocalDate to) {
		validateDateRange(from, to);
		Phong room = findRoom(roomId);
		LinkedHashMap<String, CalendarEventResponse> events = new LinkedHashMap<>();
		List<LichDangKy> schedules = scheduleRepository.findRoomCandidates(room.getId(), OCCUPYING_STATUSES, from, to);
		for (LichDangKy schedule : schedules) {
			PhieuDangKy registration = schedule.getRegistration();
			LocalDate intersectionStart = ScheduleDateCalculator.later(from, registration.getStartDate());
			LocalDate intersectionEnd = ScheduleDateCalculator.earlier(to, registration.getEndDate());
			for (LocalDate date : ScheduleDateCalculator.datesForSystemDay(intersectionStart, intersectionEnd,
					schedule.getDayOfWeek())) {
				CalendarEventResponse event = registrationEvent(schedule, date);
				events.putIfAbsent(calendarEventKey(event), event);
			}
		}

		resourceRepository.findByRoom_Id(room.getId())
				.ifPresent(resource -> addRoomBlockedEvents(from, to, blockedScheduleRepository
						.findCandidates(List.of(resource.getId()), LichChanTrangThai.HIEU_LUC, from, to), events));

		List<CalendarEventResponse> sortedEvents = events.values().stream()
				.sorted(Comparator.comparing(CalendarEventResponse::date)
						.thenComparing(event -> event.periodId() == null ? 0 : event.periodId())
						.thenComparing(event -> event.type().name()))
				.toList();
		return new RoomCalendarResponse(room.getId(), room.getName(), from, to, periods(), sortedEvents);
	}

	@Transactional(readOnly = true)
	public List<PeriodResponse> periods() {
		return periodRepository.findAllByOrderByIdAsc().stream().map(this::toPeriodResponse).toList();
	}

	private void addResourceStatusConflicts(Phong room, List<ThietBi> devices, int dayOfWeek, TietHoc period,
			Map<String, AvailabilityConflictResponse> conflicts) {
		if (room != null && room.getStatus() != PhongTrangThai.SAN_SANG) {
			addConflict(conflicts,
					new AvailabilityConflictResponse(AvailabilityConflictType.ROOM_UNAVAILABLE, LoaiTaiNguyen.PHONG,
							room.getId(), room.getName(), null, dayOfWeek, period.getId(), period.getName(),
							"Phòng đang ở trạng thái " + room.getStatus() + "."));
		}
		for (ThietBi device : devices) {
			if (device.getStatus() != ThietBiTrangThai.SAN_SANG) {
				addConflict(conflicts, new AvailabilityConflictResponse(AvailabilityConflictType.DEVICE_UNAVAILABLE,
						LoaiTaiNguyen.THIET_BI, device.getId(), device.getName(), null, dayOfWeek, period.getId(),
						period.getName(), "Thiết bị đang ở trạng thái " + device.getStatus() + "."));
			}
		}
	}

	private void addRoomRegistrationConflicts(Phong room, LocalDate from, LocalDate to, int dayOfWeek, TietHoc period,
			Map<String, AvailabilityConflictResponse> conflicts) {
		if (room == null) {
			return;
		}
		for (LichDangKy schedule : scheduleRepository.findRoomCandidates(room.getId(), OCCUPYING_STATUSES, from, to)) {
			if (schedule.getDayOfWeek() != dayOfWeek || !schedule.getPeriod().getId().equals(period.getId())) {
				continue;
			}
			for (LocalDate date : conflictDates(schedule.getRegistration(), from, to, dayOfWeek)) {
				String state = schedule.getRegistration().getStatus() == PhieuDangKyTrangThai.DANG_SU_DUNG
						? "đang sử dụng"
						: "đã duyệt";
				addConflict(conflicts,
						new AvailabilityConflictResponse(AvailabilityConflictType.ROOM_REGISTRATION,
								LoaiTaiNguyen.PHONG, room.getId(), room.getName(), date, dayOfWeek, period.getId(),
								period.getName(), "Phòng đã có lịch " + state + " trong tiết này."));
			}
		}
	}

	private void addDeviceRegistrationConflicts(List<ThietBi> devices, LocalDate from, LocalDate to, int dayOfWeek,
			TietHoc period, Map<String, AvailabilityConflictResponse> conflicts) {
		if (devices.isEmpty()) {
			return;
		}
		List<String> deviceIds = devices.stream().map(ThietBi::getId).toList();
		List<PhieuDangKyThietBi> allocations = allocationRepository.findAllocatedCandidates(deviceIds,
				OCCUPYING_STATUSES, from, to);
		Map<String, List<ThietBi>> devicesByRegistration = new LinkedHashMap<>();
		for (PhieuDangKyThietBi allocation : allocations) {
			devicesByRegistration.computeIfAbsent(allocation.getRegistration().getId(), ignored -> new ArrayList<>())
					.add(allocation.getDevice());
		}
		if (devicesByRegistration.isEmpty()) {
			return;
		}
		List<LichDangKy> schedules = scheduleRepository.findSlotCandidates(devicesByRegistration.keySet(), dayOfWeek,
				period.getId());
		for (LichDangKy schedule : schedules) {
			for (LocalDate date : conflictDates(schedule.getRegistration(), from, to, dayOfWeek)) {
				for (ThietBi device : devicesByRegistration.getOrDefault(schedule.getRegistration().getId(),
						List.of())) {
					addConflict(conflicts,
							new AvailabilityConflictResponse(AvailabilityConflictType.DEVICE_REGISTRATION,
									LoaiTaiNguyen.THIET_BI, device.getId(), device.getName(), date, dayOfWeek,
									period.getId(), period.getName(), "Thiết bị đã được phân bổ cho lịch khác."));
				}
			}
		}
	}

	private void addBlockedScheduleConflicts(Phong room, List<ThietBi> devices, LocalDate from, LocalDate to,
			int dayOfWeek, TietHoc period, Map<String, AvailabilityConflictResponse> conflicts) {
		List<TaiNguyen> resources = new ArrayList<>();
		if (room != null) {
			resourceRepository.findByRoom_Id(room.getId()).ifPresent(resources::add);
		}
		if (!devices.isEmpty()) {
			resources.addAll(resourceRepository.findAllByDevice_IdIn(devices.stream().map(ThietBi::getId).toList()));
		}
		if (resources.isEmpty()) {
			return;
		}
		List<LichChan> blockedSchedules = blockedScheduleRepository.findCandidates(
				resources.stream().map(TaiNguyen::getId).toList(), LichChanTrangThai.HIEU_LUC, from, to);
		for (LichChan blocked : blockedSchedules) {
			if (blocked.getDayOfWeek() != null && blocked.getDayOfWeek() != dayOfWeek) {
				continue;
			}
			if (blocked.getPeriod() != null && !blocked.getPeriod().getId().equals(period.getId())) {
				continue;
			}
			LocalDate intersectionStart = ScheduleDateCalculator.later(from, blocked.getStartDate());
			LocalDate intersectionEnd = ScheduleDateCalculator.earlier(to, blocked.getEndDate());
			for (LocalDate date : ScheduleDateCalculator.datesForSystemDay(intersectionStart, intersectionEnd,
					dayOfWeek)) {
				TaiNguyen resource = blocked.getResource();
				String messagePrefix = blocked.getPeriod() == null ? "Lịch chặn cả ngày: " : "Lịch chặn: ";
				addConflict(conflicts,
						new AvailabilityConflictResponse(AvailabilityConflictType.BLOCKED_SCHEDULE,
								resource.getResourceType(), resourceEntityId(resource), resourceName(resource), date,
								dayOfWeek, period.getId(), period.getName(), messagePrefix + blocked.getReason()));
			}
		}
	}

	private void addRoomBlockedEvents(LocalDate from, LocalDate to, List<LichChan> blockedSchedules,
			Map<String, CalendarEventResponse> events) {
		for (LichChan blocked : blockedSchedules) {
			LocalDate intersectionStart = ScheduleDateCalculator.later(from, blocked.getStartDate());
			LocalDate intersectionEnd = ScheduleDateCalculator.earlier(to, blocked.getEndDate());
			List<LocalDate> dates = blocked.getDayOfWeek() == null
					? ScheduleDateCalculator.datesBetween(intersectionStart, intersectionEnd)
					: ScheduleDateCalculator.datesForSystemDay(intersectionStart, intersectionEnd,
							blocked.getDayOfWeek());
			for (LocalDate date : dates) {
				TietHoc period = blocked.getPeriod();
				CalendarEventResponse event = new CalendarEventResponse(CalendarEventType.BLOCKED_SCHEDULE, date,
						ScheduleDateCalculator.toSystemDay(date), period == null ? null : period.getId(),
						period == null ? null : period.getName(), period == null ? null : period.getStartTime(),
						period == null ? null : period.getEndTime(), period == null, "Lịch chặn", blocked.getReason());
				events.putIfAbsent(calendarEventKey(event) + "|" + blocked.getId(), event);
			}
		}
	}

	private CalendarEventResponse registrationEvent(LichDangKy schedule, LocalDate date) {
		boolean inUse = schedule.getRegistration().getStatus() == PhieuDangKyTrangThai.DANG_SU_DUNG;
		TietHoc period = schedule.getPeriod();
		return new CalendarEventResponse(
				inUse ? CalendarEventType.IN_USE_REGISTRATION : CalendarEventType.APPROVED_REGISTRATION, date,
				schedule.getDayOfWeek(), period.getId(), period.getName(), period.getStartTime(), period.getEndTime(),
				false, inUse ? "Đang sử dụng" : "Lịch đã duyệt", "Phòng đã được xếp lịch trong tiết này.");
	}

	private List<LocalDate> conflictDates(PhieuDangKy registration, LocalDate from, LocalDate to, int dayOfWeek) {
		LocalDate intersectionStart = ScheduleDateCalculator.later(from, registration.getStartDate());
		LocalDate intersectionEnd = ScheduleDateCalculator.earlier(to, registration.getEndDate());
		return ScheduleDateCalculator.datesForSystemDay(intersectionStart, intersectionEnd, dayOfWeek);
	}

	private void addConflict(Map<String, AvailabilityConflictResponse> conflicts,
			AvailabilityConflictResponse conflict) {
		String key = conflict.type() + "|" + conflict.resourceType() + "|" + conflict.resourceId() + "|"
				+ conflict.date() + "|" + conflict.periodId();
		conflicts.putIfAbsent(key, conflict);
	}

	private String calendarEventKey(CalendarEventResponse event) {
		return event.type() + "|" + event.date() + "|" + event.periodId();
	}

	private List<ThietBi> findDevices(List<String> deviceIds) {
		if (deviceIds.isEmpty()) {
			return List.of();
		}
		List<ThietBi> devices = deviceRepository.findAllById(deviceIds);
		if (devices.size() != deviceIds.size()) {
			throw new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND,
					"Không tìm thấy một hoặc nhiều thiết bị.");
		}
		return devices.stream().sorted(Comparator.comparing(ThietBi::getId)).toList();
	}

	private Phong findRoom(String id) {
		String normalizedId = normalizeOptional(id);
		if (normalizedId == null) {
			throw new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Không tìm thấy phòng.");
		}
		return roomRepository.findById(normalizedId).orElseThrow(
				() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Không tìm thấy phòng."));
	}

	private TietHoc findPeriod(int id) {
		return periodRepository.findById(id).orElseThrow(
				() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Không tìm thấy tiết học."));
	}

	private List<String> normalizeDeviceIds(Collection<String> deviceIds) {
		if (deviceIds == null) {
			return List.of();
		}
		Map<String, String> uniqueIds = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (String deviceId : deviceIds) {
			String normalized = normalizeOptional(deviceId);
			if (normalized != null) {
				uniqueIds.putIfAbsent(normalized, normalized);
			}
		}
		return List.copyOf(uniqueIds.values());
	}

	private String resourceEntityId(TaiNguyen resource) {
		return resource.getResourceType() == LoaiTaiNguyen.PHONG
				? resource.getRoom().getId()
				: resource.getDevice().getId();
	}

	private String resourceName(TaiNguyen resource) {
		return resource.getResourceType() == LoaiTaiNguyen.PHONG
				? resource.getRoom().getName()
				: resource.getDevice().getName();
	}

	private PeriodResponse toPeriodResponse(TietHoc period) {
		return new PeriodResponse(period.getId(), period.getName(), period.getStartTime(), period.getEndTime());
	}

	private void validateDateQuery(LocalDate from, LocalDate to, int dayOfWeek) {
		validateDateRange(from, to);
		try {
			ScheduleDateCalculator.validateSystemDay(dayOfWeek);
		} catch (IllegalArgumentException exception) {
			throw validation(exception.getMessage());
		}
	}

	private void validateDateRange(LocalDate from, LocalDate to) {
		try {
			ScheduleDateCalculator.validateRange(from, to);
		} catch (IllegalArgumentException exception) {
			throw validation(exception.getMessage());
		}
	}

	private String normalizeOptional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private ApiException validation(String message) {
		return new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, message);
	}
}
