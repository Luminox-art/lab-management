package com.example.labmanagement.usage.application;

import com.example.labmanagement.catalog.domain.TaiNguyen;
import com.example.labmanagement.catalog.domain.ThietBi;
import com.example.labmanagement.catalog.domain.ThietBiTrangThai;
import com.example.labmanagement.catalog.persistence.TaiNguyenRepository;
import com.example.labmanagement.catalog.persistence.ThietBiRepository;
import com.example.labmanagement.common.clock.TimeConfiguration;
import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.common.error.ErrorCode;
import com.example.labmanagement.identity.domain.NguoiDung;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.persistence.NguoiDungRepository;
import com.example.labmanagement.incident.domain.SuCo;
import com.example.labmanagement.incident.domain.SuCoTrangThai;
import com.example.labmanagement.incident.persistence.SuCoRepository;
import com.example.labmanagement.registration.domain.PhieuDangKy;
import com.example.labmanagement.registration.domain.PhieuDangKyThietBi;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import com.example.labmanagement.registration.persistence.PhieuDangKyThietBiRepository;
import com.example.labmanagement.usage.domain.PhienSuDung;
import com.example.labmanagement.usage.domain.PhienSuDungThietBi;
import com.example.labmanagement.usage.domain.PhienSuDungTrangThai;
import com.example.labmanagement.usage.persistence.PhienSuDungRepository;
import com.example.labmanagement.usage.persistence.PhienSuDungThietBiRepository;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsageSessionService {

	private static final String ROLE_MANAGER = "CBQL";
	private static final int CHECK_IN_EARLY_MINUTES = 30;

	private final NguoiDungRepository userRepository;
	private final PhienSuDungRepository sessionRepository;
	private final PhienSuDungThietBiRepository sessionDeviceRepository;
	private final PhieuDangKyThietBiRepository allocationRepository;
	private final ThietBiRepository deviceRepository;
	private final TaiNguyenRepository resourceRepository;
	private final SuCoRepository incidentRepository;
	private final EntityManager entityManager;
	private final Clock clock;

	public UsageSessionService(NguoiDungRepository userRepository, PhienSuDungRepository sessionRepository,
			PhienSuDungThietBiRepository sessionDeviceRepository, PhieuDangKyThietBiRepository allocationRepository,
			ThietBiRepository deviceRepository, TaiNguyenRepository resourceRepository,
			SuCoRepository incidentRepository, EntityManager entityManager, Clock clock) {
		this.userRepository = userRepository;
		this.sessionRepository = sessionRepository;
		this.sessionDeviceRepository = sessionDeviceRepository;
		this.allocationRepository = allocationRepository;
		this.deviceRepository = deviceRepository;
		this.resourceRepository = resourceRepository;
		this.incidentRepository = incidentRepository;
		this.entityManager = entityManager;
		this.clock = clock;
	}

	@Transactional
	public List<UsageSessionResponse> listAccessible(String actorEmail, LocalDate from, LocalDate to) {
		NguoiDung actor = findActor(actorEmail);
		LocalDate today = ZonedDateTime.now(clock.withZone(TimeConfiguration.DISPLAY_ZONE)).toLocalDate();
		LocalDate normalizedFrom = from == null ? today.minusDays(7) : from;
		LocalDate normalizedTo = to == null ? today.plusDays(30) : to;
		if (normalizedFrom.isAfter(normalizedTo)) {
			throw validation("Ngày bắt đầu phải không sau ngày kết thúc.");
		}
		markOverdueSessionsInternal();
		List<PhienSuDung> sessions = isManager(actor)
				? sessionRepository.findAccessibleToManager(normalizedFrom, normalizedTo)
				: sessionRepository.findAccessibleByCreatorId(actor.getId(), normalizedFrom, normalizedTo);
		return sessions.stream().map(session -> response(session, List.of())).toList();
	}

	@Transactional(readOnly = true)
	public UsageSessionResponse get(String actorEmail, Long sessionId) {
		NguoiDung actor = findActor(actorEmail);
		PhienSuDung session = sessionRepository.findDetailById(sessionId)
				.orElseThrow(() -> notFound("Không tìm thấy phiên sử dụng."));
		assertAccess(actor, session);
		return response(session, List.of());
	}

	@Transactional
	public UsageSessionResponse checkIn(String actorEmail, Long sessionId, SessionCheckInRequest request) {
		NguoiDung actor = findActor(actorEmail);
		if (request == null || request.version() == null || request.devices() == null) {
			throw validation("Version và tình trạng thiết bị không được để trống.");
		}
		PhienSuDung session = lockSession(sessionId);
		assertAccess(actor, session);
		if (session.getStatus() != PhienSuDungTrangThai.CHUA_BAT_DAU) {
			throw conflict("Phiên không còn ở trạng thái chưa bắt đầu.");
		}
		assertVersion(session, request.version());

		PhieuDangKy registration = session.getSchedule().getRegistration();
		if (registration.getStatus() != PhieuDangKyTrangThai.DA_DUYET) {
			throw conflict("Phiếu phải ở trạng thái đã duyệt trước khi check-in.");
		}
		assertCheckInWindow(session);

		List<PhieuDangKyThietBi> allocations = allocationRepository.findAllocatedByRegistrationId(registration.getId());
		Map<String, SessionDeviceConditionRequest> conditions = normalizeConditions(request.devices());
		assertExactDeviceSet(allocations.stream().map(item -> item.getDevice().getId()).toList(), conditions.keySet());
		List<ThietBi> devices = lockDevices(conditions.keySet());
		for (ThietBi device : devices) {
			if (device.getStatus() != ThietBiTrangThai.SAN_SANG) {
				throw conflict("Thiết bị " + device.getId() + " không ở trạng thái sẵn sàng.");
			}
		}

		Instant now = clock.instant();
		session.checkIn(actor, now);
		registration.startUsing(now);
		List<PhienSuDungThietBi> sessionDevices = new ArrayList<>();
		for (ThietBi device : devices) {
			SessionDeviceConditionRequest condition = conditions.get(device.getId());
			device.startUsing();
			sessionDevices.add(new PhienSuDungThietBi(session, device, condition.condition(), null,
					normalizeOptional(condition.note())));
		}
		sessionDeviceRepository.saveAll(sessionDevices);
		sessionRepository.flush();
		entityManager.refresh(session);
		return response(session, List.of());
	}

	@Transactional
	public UsageSessionResponse checkOut(String actorEmail, Long sessionId, SessionCheckOutRequest request) {
		NguoiDung actor = findActor(actorEmail);
		if (request == null || request.version() == null || request.devices() == null || request.incidents() == null) {
			throw validation("Version, tình trạng trả và danh sách sự cố không được để trống.");
		}
		PhienSuDung session = lockSession(sessionId);
		assertAccess(actor, session);
		if (session.getStatus() != PhienSuDungTrangThai.DANG_SU_DUNG) {
			throw conflict("Phiên không ở trạng thái đang sử dụng.");
		}
		assertVersion(session, request.version());
		assertUsageDay(session);

		List<PhienSuDungThietBi> sessionDevices = sessionDeviceRepository.findAllBySessionIdForUpdate(session.getId());
		Map<String, SessionDeviceConditionRequest> conditions = normalizeConditions(request.devices());
		List<String> deviceIds = sessionDevices.stream().map(item -> item.getDevice().getId()).toList();
		assertExactDeviceSet(deviceIds, conditions.keySet());
		Map<String, SessionIncidentRequest> incidents = normalizeIncidents(request.incidents(), deviceIds);
		List<ThietBi> devices = lockDevices(deviceIds);
		Map<String, TaiNguyen> resources = resourcesByDeviceId(incidents.keySet());

		Instant now = clock.instant();
		session.checkOut(actor, now);
		for (PhienSuDungThietBi item : sessionDevices) {
			SessionDeviceConditionRequest condition = conditions.get(item.getDevice().getId());
			item.recordReturn(condition.condition(), normalizeOptional(condition.note()));
		}
		for (ThietBi device : devices) {
			device.finishUsing(incidents.containsKey(device.getId()));
		}

		List<SuCo> createdIncidents = new ArrayList<>();
		for (SessionIncidentRequest incident : incidents.values()) {
			createdIncidents.add(new SuCo("SC-" + UUID.randomUUID(), resources.get(incident.deviceId()), session, actor,
					null, incident.severity(), incident.description(), SuCoTrangThai.MOI, now, null, null));
		}
		incidentRepository.saveAll(createdIncidents);
		updateRegistrationAfterTerminalSession(session.getSchedule().getRegistration(), now);
		sessionRepository.flush();
		entityManager.refresh(session);
		return response(session, createdIncidents.stream().map(SuCo::getId).toList());
	}

	@Transactional
	public int markOverdueSessions() {
		return markOverdueSessionsInternal();
	}

	private int markOverdueSessionsInternal() {
		ZonedDateTime now = ZonedDateTime.now(clock.withZone(TimeConfiguration.DISPLAY_ZONE));
		List<PhienSuDung> candidates = sessionRepository
				.findOverdueCandidatesForUpdate(PhienSuDungTrangThai.CHUA_BAT_DAU, now.toLocalDate());
		Map<String, PhieuDangKy> affected = new LinkedHashMap<>();
		int count = 0;
		for (PhienSuDung session : candidates) {
			boolean pastDay = session.getUsageDate().isBefore(now.toLocalDate());
			boolean endedToday = session.getUsageDate().isEqual(now.toLocalDate())
					&& now.toLocalTime().isAfter(session.getSchedule().getPeriod().getEndTime());
			if (pastDay || endedToday) {
				session.markAbsent();
				PhieuDangKy registration = session.getSchedule().getRegistration();
				affected.put(registration.getId(), registration);
				count++;
			}
		}
		Instant instant = clock.instant();
		for (PhieuDangKy registration : affected.values()) {
			updateRegistrationAfterTerminalSession(registration, instant);
		}
		return count;
	}

	private void updateRegistrationAfterTerminalSession(PhieuDangKy registration, Instant now) {
		if (sessionRepository.existsBySchedule_Registration_IdAndStatus(registration.getId(),
				PhienSuDungTrangThai.DANG_SU_DUNG)) {
			if (registration.getStatus() != PhieuDangKyTrangThai.DANG_SU_DUNG) {
				registration.startUsing(now);
			}
			return;
		}
		if (sessionRepository.existsBySchedule_Registration_IdAndStatus(registration.getId(),
				PhienSuDungTrangThai.CHUA_BAT_DAU)) {
			if (registration.getStatus() == PhieuDangKyTrangThai.DANG_SU_DUNG) {
				registration.returnToApproved(now);
			}
			return;
		}
		if (registration.getStatus() != PhieuDangKyTrangThai.HOAN_THANH) {
			registration.complete(now);
		}
	}

	private UsageSessionResponse response(PhienSuDung session, List<String> incidentIds) {
		List<PhienSuDungThietBi> recorded = sessionDeviceRepository.findAllBySessionId(session.getId());
		List<SessionDeviceResponse> devices;
		if (recorded.isEmpty()) {
			devices = allocationRepository
					.findAllocatedByRegistrationId(session.getSchedule().getRegistration().getId()).stream()
					.map(item -> new SessionDeviceResponse(item.getDevice().getId(), item.getDevice().getName(),
							item.getDevice().getType().getName(), null, null, null))
					.toList();
		} else {
			devices = recorded.stream()
					.map(item -> new SessionDeviceResponse(item.getDevice().getId(), item.getDevice().getName(),
							item.getDevice().getType().getName(), item.getReceivedCondition(),
							item.getReturnedCondition(), item.getNote()))
					.toList();
		}
		PhieuDangKy registration = session.getSchedule().getRegistration();
		ZonedDateTime now = ZonedDateTime.now(clock.withZone(TimeConfiguration.DISPLAY_ZONE));
		boolean canCheckIn = registration.getStatus() == PhieuDangKyTrangThai.DA_DUYET
				&& session.getStatus() == PhienSuDungTrangThai.CHUA_BAT_DAU && isWithinCheckInWindow(session, now);
		boolean canCheckOut = registration.getStatus() == PhieuDangKyTrangThai.DANG_SU_DUNG
				&& session.getStatus() == PhienSuDungTrangThai.DANG_SU_DUNG
				&& session.getUsageDate().isEqual(now.toLocalDate());
		return new UsageSessionResponse(session.getId(), session.getVersion(), registration.getId(),
				session.getUsageDate(), session.getStatus(), registration.getRoom().getId(),
				registration.getRoom().getName(), registration.getCreator().getId(),
				registration.getCreator().getFullName(), session.getSchedule().getPeriod().getId(),
				session.getSchedule().getPeriod().getName(), session.getSchedule().getPeriod().getStartTime(),
				session.getSchedule().getPeriod().getEndTime(), displayTime(session.getCheckedInAt()),
				session.getCheckedInBy() == null ? null : session.getCheckedInBy().getFullName(),
				displayTime(session.getCheckedOutAt()),
				session.getCheckedOutBy() == null ? null : session.getCheckedOutBy().getFullName(), devices,
				List.copyOf(incidentIds), canCheckIn, canCheckOut);
	}

	private Map<String, SessionDeviceConditionRequest> normalizeConditions(
			Collection<SessionDeviceConditionRequest> values) {
		Map<String, SessionDeviceConditionRequest> normalized = new LinkedHashMap<>();
		for (SessionDeviceConditionRequest value : values) {
			if (value == null) {
				throw validation("Tình trạng thiết bị không hợp lệ.");
			}
			String deviceId = normalizeRequired(value.deviceId(), "Mã thiết bị không được để trống.");
			String condition = normalizeRequired(value.condition(), "Tình trạng thiết bị không được để trống.");
			if (deviceId.length() > 50 || condition.length() > 255) {
				throw validation("Mã hoặc tình trạng thiết bị vượt quá độ dài cho phép.");
			}
			if (normalized.putIfAbsent(deviceId,
					new SessionDeviceConditionRequest(deviceId, condition, normalizeOptional(value.note()))) != null) {
				throw validation("Danh sách tình trạng có mã thiết bị bị trùng.");
			}
		}
		return normalized;
	}

	private Map<String, SessionIncidentRequest> normalizeIncidents(Collection<SessionIncidentRequest> values,
			Collection<String> allowedDeviceIds) {
		Set<String> allowed = new HashSet<>(allowedDeviceIds);
		Map<String, SessionIncidentRequest> normalized = new LinkedHashMap<>();
		for (SessionIncidentRequest value : values) {
			if (value == null || value.severity() == null) {
				throw validation("Thông tin sự cố không hợp lệ.");
			}
			String deviceId = normalizeRequired(value.deviceId(), "Mã thiết bị sự cố không được để trống.");
			String description = normalizeRequired(value.description(), "Mô tả sự cố không được để trống.");
			if (!allowed.contains(deviceId)) {
				throw validation("Chỉ được báo sự cố cho thiết bị thuộc phiên.");
			}
			if (description.length() > 2000) {
				throw validation("Mô tả sự cố không được vượt quá 2000 ký tự.");
			}
			if (normalized.putIfAbsent(deviceId,
					new SessionIncidentRequest(deviceId, value.severity(), description)) != null) {
				throw validation("Mỗi thiết bị chỉ được tạo một sự cố khi check-out.");
			}
		}
		return normalized;
	}

	private Map<String, TaiNguyen> resourcesByDeviceId(Collection<String> deviceIds) {
		if (deviceIds.isEmpty()) {
			return Map.of();
		}
		Map<String, TaiNguyen> resources = new HashMap<>();
		for (TaiNguyen resource : resourceRepository.findAllByDevice_IdIn(deviceIds)) {
			resources.put(resource.getDevice().getId(), resource);
		}
		if (!resources.keySet().containsAll(deviceIds)) {
			throw conflict("Có thiết bị chưa được cấu hình tài nguyên để ghi nhận sự cố.");
		}
		return resources;
	}

	private List<ThietBi> lockDevices(Collection<String> deviceIds) {
		if (deviceIds.isEmpty()) {
			return List.of();
		}
		List<ThietBi> devices = deviceRepository.findAllByIdForUpdateOrderById(deviceIds);
		if (devices.size() != deviceIds.size()) {
			throw notFound("Không tìm thấy đầy đủ thiết bị của phiên.");
		}
		return devices;
	}

	private void assertExactDeviceSet(Collection<String> expected, Collection<String> actual) {
		if (expected.size() != actual.size() || !new HashSet<>(expected).equals(new HashSet<>(actual))) {
			throw validation("Phải khai báo tình trạng cho đúng và đủ thiết bị đã phân bổ.");
		}
	}

	private void assertCheckInWindow(PhienSuDung session) {
		ZonedDateTime now = ZonedDateTime.now(clock.withZone(TimeConfiguration.DISPLAY_ZONE));
		if (!isWithinCheckInWindow(session, now)) {
			throw conflict("Chỉ được check-in từ 30 phút trước giờ bắt đầu đến hết tiết của đúng ngày sử dụng.");
		}
	}

	private boolean isWithinCheckInWindow(PhienSuDung session, ZonedDateTime now) {
		if (!session.getUsageDate().isEqual(now.toLocalDate())) {
			return false;
		}
		return !now.toLocalTime()
				.isBefore(session.getSchedule().getPeriod().getStartTime().minusMinutes(CHECK_IN_EARLY_MINUTES))
				&& !now.toLocalTime().isAfter(session.getSchedule().getPeriod().getEndTime());
	}

	private void assertUsageDay(PhienSuDung session) {
		LocalDate today = ZonedDateTime.now(clock.withZone(TimeConfiguration.DISPLAY_ZONE)).toLocalDate();
		if (!session.getUsageDate().isEqual(today)) {
			throw conflict("Chỉ được check-out trong ngày sử dụng của phiên.");
		}
	}

	private void assertVersion(PhienSuDung session, long version) {
		if (session.getVersion() != version) {
			throw conflict("Phiên đã được cập nhật bởi yêu cầu khác.");
		}
	}

	private void assertAccess(NguoiDung actor, PhienSuDung session) {
		if (!isManager(actor) && !actor.getId().equals(session.getSchedule().getRegistration().getCreator().getId())) {
			throw new ApiException(ErrorCode.ACCESS_DENIED, HttpStatus.FORBIDDEN,
					"Bạn không có quyền thao tác phiên sử dụng này.");
		}
	}

	private NguoiDung findActor(String email) {
		String normalized = normalizeOptional(email);
		NguoiDung actor = normalized == null
				? null
				: userRepository.findByEmailIgnoreCase(normalized.toLowerCase(Locale.ROOT)).orElse(null);
		if (actor == null || actor.getStatus() != NguoiDungTrangThai.HOAT_DONG) {
			throw new ApiException(ErrorCode.ACCESS_DENIED, HttpStatus.FORBIDDEN,
					"Tài khoản không hoạt động hoặc không tồn tại.");
		}
		return actor;
	}

	private boolean isManager(NguoiDung actor) {
		return ROLE_MANAGER.equals(actor.getRole().getId());
	}

	private PhienSuDung lockSession(Long sessionId) {
		if (sessionId == null) {
			throw notFound("Không tìm thấy phiên sử dụng.");
		}
		return sessionRepository.findDetailByIdForUpdate(sessionId)
				.orElseThrow(() -> notFound("Không tìm thấy phiên sử dụng."));
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

	private java.time.OffsetDateTime displayTime(Instant instant) {
		return instant == null ? null : instant.atZone(TimeConfiguration.DISPLAY_ZONE).toOffsetDateTime();
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
}
