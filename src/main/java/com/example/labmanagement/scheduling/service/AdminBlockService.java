package com.example.labmanagement.scheduling.service;

import com.example.labmanagement.catalog.domain.LoaiTaiNguyen;
import com.example.labmanagement.catalog.domain.TaiNguyen;
import com.example.labmanagement.catalog.repository.PhongRepository;
import com.example.labmanagement.catalog.repository.TaiNguyenRepository;
import com.example.labmanagement.catalog.repository.ThietBiRepository;
import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.common.error.ErrorCode;
import com.example.labmanagement.identity.domain.NguoiDung;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.domain.RolePolicy;
import com.example.labmanagement.identity.repository.NguoiDungRepository;
import com.example.labmanagement.registration.domain.LichDangKy;
import com.example.labmanagement.registration.domain.PhieuDangKyThietBi;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import com.example.labmanagement.registration.repository.LichDangKyRepository;
import com.example.labmanagement.registration.repository.PhieuDangKyThietBiRepository;
import com.example.labmanagement.scheduling.domain.LichChan;
import com.example.labmanagement.scheduling.domain.LichChanTrangThai;
import com.example.labmanagement.scheduling.domain.ScheduleDateCalculator;
import com.example.labmanagement.scheduling.domain.TietHoc;
import com.example.labmanagement.scheduling.dto.AdminBlockCreationResponse;
import com.example.labmanagement.scheduling.dto.AdminBlockRequest;
import com.example.labmanagement.scheduling.dto.AdminBlockResponse;
import com.example.labmanagement.scheduling.repository.LichChanRepository;
import com.example.labmanagement.scheduling.repository.TietHocRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminBlockService {

	private static final String EMPTY_ROOM_LOCK_KEY = "__NO_ROOM__";
	private static final String EMPTY_DEVICE_LOCK_KEY = "__NO_DEVICE__";
	private static final Set<PhieuDangKyTrangThai> OCCUPYING_STATUSES = Set.of(PhieuDangKyTrangThai.DA_DUYET,
			PhieuDangKyTrangThai.DANG_SU_DUNG);

	private final NguoiDungRepository userRepository;
	private final PhongRepository roomRepository;
	private final ThietBiRepository deviceRepository;
	private final TaiNguyenRepository resourceRepository;
	private final TietHocRepository periodRepository;
	private final LichDangKyRepository scheduleRepository;
	private final PhieuDangKyThietBiRepository allocationRepository;
	private final LichChanRepository blockedScheduleRepository;
	private final EntityManager entityManager;

	public AdminBlockService(NguoiDungRepository userRepository, PhongRepository roomRepository,
			ThietBiRepository deviceRepository, TaiNguyenRepository resourceRepository,
			TietHocRepository periodRepository, LichDangKyRepository scheduleRepository,
			PhieuDangKyThietBiRepository allocationRepository, LichChanRepository blockedScheduleRepository,
			EntityManager entityManager) {
		this.userRepository = userRepository;
		this.roomRepository = roomRepository;
		this.deviceRepository = deviceRepository;
		this.resourceRepository = resourceRepository;
		this.periodRepository = periodRepository;
		this.scheduleRepository = scheduleRepository;
		this.allocationRepository = allocationRepository;
		this.blockedScheduleRepository = blockedScheduleRepository;
		this.entityManager = entityManager;
	}

	@Transactional(readOnly = true)
	public List<AdminBlockResponse> findAll(String actorEmail) {
		findManager(actorEmail);
		return blockedScheduleRepository.findAllByOrderByStartDateDescIdDesc().stream().map(this::toResponse).toList();
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public AdminBlockCreationResponse create(String actorEmail, AdminBlockRequest request) {
		NguoiDung manager = findManager(actorEmail);
		ValidatedRequest validated = validate(request);
		TaiNguyen resource = lockResource(validated.roomId(), validated.deviceId());
		List<TietHoc> periods = findPeriods(validated.periodIds());

		assertNoEffectiveBlockConflict(resource, validated, periods);
		assertNoRegistrationConflict(resource, validated, periods);

		Byte storedDay = validated.dayOfWeek() == null ? null : validated.dayOfWeek().byteValue();
		List<LichChan> created = periods.isEmpty()
				? List.of(blockedScheduleRepository.save(new LichChan(resource, validated.startDate(),
						validated.endDate(), storedDay, null, validated.reason(), LichChanTrangThai.HIEU_LUC, manager)))
				: periods.stream()
						.map(period -> blockedScheduleRepository
								.save(new LichChan(resource, validated.startDate(), validated.endDate(), storedDay,
										period, validated.reason(), LichChanTrangThai.HIEU_LUC, manager)))
						.toList();
		blockedScheduleRepository.flush();
		return new AdminBlockCreationResponse(created.stream().map(this::toResponse).toList());
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void cancel(String actorEmail, long id) {
		findManager(actorEmail);
		LichChan current = blockedScheduleRepository.findDetailById(id)
				.orElseThrow(() -> notFound("Không tìm thấy lịch chặn."));
		lockResource(resourceRoomId(current.getResource()), resourceDeviceId(current.getResource()));
		LichChan blocked = blockedScheduleRepository.findByIdForUpdate(id)
				.orElseThrow(() -> notFound("Không tìm thấy lịch chặn."));
		entityManager.refresh(blocked);
		if (blocked.getStatus() != LichChanTrangThai.HIEU_LUC) {
			throw conflict("Lịch chặn đã được hủy trước đó.");
		}
		blocked.cancel();
		blockedScheduleRepository.flush();
	}

	private ValidatedRequest validate(AdminBlockRequest request) {
		if (request == null) {
			throw validation("Dữ liệu lịch chặn không được để trống.");
		}
		String roomId = normalizeOptional(request.roomId());
		String deviceId = normalizeOptional(request.deviceId());
		if ((roomId == null) == (deviceId == null)) {
			throw validation("Phải chọn đúng một phòng hoặc một thiết bị.");
		}
		try {
			ScheduleDateCalculator.validateRange(request.startDate(), request.endDate());
			if (request.dayOfWeek() != null) {
				ScheduleDateCalculator.validateSystemDay(request.dayOfWeek());
				if (ScheduleDateCalculator
						.datesForSystemDay(request.startDate(), request.endDate(), request.dayOfWeek()).isEmpty()) {
					throw validation("Khoảng ngày không chứa thứ đã chọn.");
				}
			}
		} catch (IllegalArgumentException exception) {
			throw validation(exception.getMessage());
		}
		String reason = normalizeOptional(request.reason());
		if (reason == null) {
			throw validation("Lý do chặn không được để trống.");
		}
		return new ValidatedRequest(roomId, deviceId, request.startDate(), request.endDate(), request.dayOfWeek(),
				normalizePeriodIds(request.periodIds()), reason);
	}

	private List<Integer> normalizePeriodIds(Collection<Integer> values) {
		if (values == null || values.isEmpty()) {
			return List.of();
		}
		Map<Integer, Integer> unique = new LinkedHashMap<>();
		for (Integer value : values) {
			if (value == null || value <= 0) {
				throw validation("Mã tiết học phải là số nguyên dương.");
			}
			if (unique.putIfAbsent(value, value) != null) {
				throw validation("Danh sách tiết học có mã bị trùng.");
			}
		}
		return unique.values().stream().sorted().toList();
	}

	private List<TietHoc> findPeriods(List<Integer> ids) {
		if (ids.isEmpty()) {
			return List.of();
		}
		List<TietHoc> periods = periodRepository.findAllById(ids).stream().sorted(Comparator.comparing(TietHoc::getId))
				.toList();
		if (periods.size() != ids.size()) {
			throw notFound("Không tìm thấy một hoặc nhiều tiết học.");
		}
		return periods;
	}

	private TaiNguyen lockResource(String roomId, String deviceId) {
		List<TaiNguyen> resources;
		if (roomId != null) {
			roomRepository.findByIdForUpdate(roomId).orElseThrow(() -> notFound("Không tìm thấy phòng."));
			resources = resourceRepository.lockForScheduling(roomId, List.of(EMPTY_DEVICE_LOCK_KEY));
		} else {
			deviceRepository.findByIdForUpdate(deviceId).orElseThrow(() -> notFound("Không tìm thấy thiết bị."));
			resources = resourceRepository.lockForScheduling(EMPTY_ROOM_LOCK_KEY, List.of(deviceId));
		}
		if (resources.size() != 1) {
			throw conflict("Tài nguyên phòng hoặc thiết bị chưa được cấu hình đầy đủ.");
		}
		return resources.getFirst();
	}

	private void assertNoEffectiveBlockConflict(TaiNguyen resource, ValidatedRequest request, List<TietHoc> periods) {
		List<LichChan> candidates = blockedScheduleRepository.findCandidates(List.of(resource.getId()),
				LichChanTrangThai.HIEU_LUC, request.startDate(), request.endDate());
		for (LichChan candidate : candidates) {
			Integer candidateDay = candidate.getDayOfWeek() == null ? null : candidate.getDayOfWeek().intValue();
			if (datePatternsOverlap(request.startDate(), request.endDate(), request.dayOfWeek(),
					candidate.getStartDate(), candidate.getEndDate(), candidateDay)
					&& requestedPeriodsOverlap(periods, candidate.getPeriod())) {
				throw conflict("Tài nguyên đã có lịch chặn hiệu lực giao với thời gian yêu cầu.");
			}
		}
	}

	private void assertNoRegistrationConflict(TaiNguyen resource, ValidatedRequest request, List<TietHoc> periods) {
		List<LichDangKy> schedules;
		if (resource.getResourceType() == LoaiTaiNguyen.PHONG) {
			schedules = scheduleRepository.findRoomCandidates(resource.getRoom().getId(), OCCUPYING_STATUSES,
					request.startDate(), request.endDate());
		} else {
			List<PhieuDangKyThietBi> allocations = allocationRepository.findAllocatedCandidates(
					List.of(resource.getDevice().getId()), OCCUPYING_STATUSES, request.startDate(), request.endDate());
			List<String> registrationIds = allocations.stream().map(item -> item.getRegistration().getId()).distinct()
					.toList();
			schedules = registrationIds.isEmpty()
					? List.of()
					: scheduleRepository.findAllByRegistrationIdIn(registrationIds);
		}
		for (LichDangKy schedule : schedules) {
			if (datePatternsOverlap(request.startDate(), request.endDate(), request.dayOfWeek(),
					schedule.getRegistration().getStartDate(), schedule.getRegistration().getEndDate(),
					(int) schedule.getDayOfWeek()) && requestedPeriodsOverlap(periods, schedule.getPeriod())) {
				throw conflict("Tài nguyên đã có phiếu được duyệt hoặc đang sử dụng giao với thời gian yêu cầu.");
			}
		}
	}

	private boolean requestedPeriodsOverlap(List<TietHoc> requestedPeriods, TietHoc otherPeriod) {
		return requestedPeriods.isEmpty() || otherPeriod == null
				|| requestedPeriods.stream().anyMatch(period -> period.getId().equals(otherPeriod.getId()));
	}

	private boolean datePatternsOverlap(LocalDate firstStart, LocalDate firstEnd, Integer firstDay,
			LocalDate secondStart, LocalDate secondEnd, Integer secondDay) {
		LocalDate intersectionStart = ScheduleDateCalculator.later(firstStart, secondStart);
		LocalDate intersectionEnd = ScheduleDateCalculator.earlier(firstEnd, secondEnd);
		if (intersectionStart.isAfter(intersectionEnd)
				|| firstDay != null && secondDay != null && !firstDay.equals(secondDay)) {
			return false;
		}
		Integer effectiveDay = firstDay == null ? secondDay : firstDay;
		return effectiveDay == null || !ScheduleDateCalculator
				.datesForSystemDay(intersectionStart, intersectionEnd, effectiveDay).isEmpty();
	}

	private AdminBlockResponse toResponse(LichChan blocked) {
		TaiNguyen resource = blocked.getResource();
		TietHoc period = blocked.getPeriod();
		Integer day = blocked.getDayOfWeek() == null ? null : blocked.getDayOfWeek().intValue();
		return new AdminBlockResponse(blocked.getId(), resource.getResourceType(), resourceEntityId(resource),
				resourceName(resource), blocked.getStartDate(), blocked.getEndDate(), day,
				day == null ? "Mỗi ngày" : ScheduleDateCalculator.systemDayLabel(day),
				period == null ? null : period.getId(), period == null ? "Cả ngày" : period.getName(),
				blocked.getReason(), blocked.getStatus(), blocked.getCreator().getId(),
				blocked.getCreator().getFullName());
	}

	private NguoiDung findManager(String email) {
		String normalized = normalizeOptional(email);
		NguoiDung actor = normalized == null
				? null
				: userRepository.findByEmailIgnoreCase(normalized.toLowerCase(Locale.ROOT)).orElse(null);
		if (actor == null || actor.getStatus() != NguoiDungTrangThai.HOAT_DONG
				|| !RolePolicy.isManager(actor.getRole().getId())) {
			throw new ApiException(ErrorCode.ACCESS_DENIED, HttpStatus.FORBIDDEN,
					"Chỉ cán bộ quản lý đang hoạt động được quản lý lịch chặn.");
		}
		return actor;
	}

	private String resourceRoomId(TaiNguyen resource) {
		return resource.getResourceType() == LoaiTaiNguyen.PHONG ? resource.getRoom().getId() : null;
	}

	private String resourceDeviceId(TaiNguyen resource) {
		return resource.getResourceType() == LoaiTaiNguyen.THIET_BI ? resource.getDevice().getId() : null;
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

	private String normalizeOptional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private ApiException validation(String message) {
		return new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, message);
	}

	private ApiException conflict(String message) {
		return new ApiException(ErrorCode.RESOURCE_CONFLICT, HttpStatus.CONFLICT, message);
	}

	private ApiException notFound(String message) {
		return new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, message);
	}

	private record ValidatedRequest(String roomId, String deviceId, LocalDate startDate, LocalDate endDate,
			Integer dayOfWeek, List<Integer> periodIds, String reason) {
	}
}
