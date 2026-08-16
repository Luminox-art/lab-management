package com.example.labmanagement.incident.service;

import com.example.labmanagement.catalog.domain.LoaiTaiNguyen;
import com.example.labmanagement.catalog.domain.TaiNguyen;
import com.example.labmanagement.catalog.repository.TaiNguyenRepository;
import com.example.labmanagement.common.clock.TimeConfiguration;
import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.common.error.ErrorCode;
import com.example.labmanagement.identity.domain.NguoiDung;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.domain.RolePolicy;
import com.example.labmanagement.identity.repository.NguoiDungRepository;
import com.example.labmanagement.incident.domain.MucDoSuCo;
import com.example.labmanagement.incident.domain.SuCo;
import com.example.labmanagement.incident.domain.SuCoTrangThai;
import com.example.labmanagement.incident.dto.IncidentCreateRequest;
import com.example.labmanagement.incident.dto.IncidentHandlerOptionResponse;
import com.example.labmanagement.incident.dto.IncidentResourceOptionResponse;
import com.example.labmanagement.incident.dto.IncidentResponse;
import com.example.labmanagement.incident.dto.IncidentUpdateRequest;
import com.example.labmanagement.incident.repository.SuCoRepository;
import com.example.labmanagement.registration.domain.PhieuDangKy;
import com.example.labmanagement.registration.repository.PhieuDangKyThietBiRepository;
import com.example.labmanagement.registration.repository.PhieuHuongDanRepository;
import com.example.labmanagement.usage.domain.PhienSuDung;
import com.example.labmanagement.usage.repository.PhienSuDungRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentService {

	private static final int MAX_PAGE_SIZE = 100;

	private final NguoiDungRepository userRepository;
	private final TaiNguyenRepository resourceRepository;
	private final PhienSuDungRepository sessionRepository;
	private final PhieuDangKyThietBiRepository allocationRepository;
	private final PhieuHuongDanRepository supervisionRepository;
	private final SuCoRepository incidentRepository;
	private final Clock clock;

	public IncidentService(NguoiDungRepository userRepository, TaiNguyenRepository resourceRepository,
			PhienSuDungRepository sessionRepository, PhieuDangKyThietBiRepository allocationRepository,
			PhieuHuongDanRepository supervisionRepository, SuCoRepository incidentRepository, Clock clock) {
		this.userRepository = userRepository;
		this.resourceRepository = resourceRepository;
		this.sessionRepository = sessionRepository;
		this.allocationRepository = allocationRepository;
		this.supervisionRepository = supervisionRepository;
		this.incidentRepository = incidentRepository;
		this.clock = clock;
	}

	@Transactional
	public IncidentResponse report(String actorEmail, IncidentCreateRequest request) {
		NguoiDung actor = findActiveActor(actorEmail);
		if (request == null || request.severity() == null) {
			throw validation("Tài nguyên, mức độ và mô tả sự cố không được để trống.");
		}
		String resourceId = normalizeRequired(request.resourceId(), "Tài nguyên không được để trống.");
		String description = normalizeRequired(request.description(), "Mô tả sự cố không được để trống.");
		if (description.length() > 2000) {
			throw validation("Mô tả sự cố không được vượt quá 2.000 ký tự.");
		}
		TaiNguyen resource = resourceRepository.findById(resourceId)
				.orElseThrow(() -> notFound("Không tìm thấy tài nguyên."));
		PhienSuDung session = request.sessionId() == null ? null : findSession(request.sessionId());
		if (session != null) {
			assertSessionAccess(actor, session);
			assertResourceBelongsToSession(resource, session);
		}
		if (resource.getResourceType() == LoaiTaiNguyen.THIET_BI) {
			resource.getDevice().finishUsing(true);
		}
		SuCo incident = incidentRepository.save(new SuCo("SC-" + UUID.randomUUID(), resource, session, actor, null,
				request.severity(), description, SuCoTrangThai.MOI, clock.instant(), null, null));
		return response(incident);
	}

	@Transactional(readOnly = true)
	public Page<IncidentResponse> search(String actorEmail, SuCoTrangThai status, MucDoSuCo severity, String resourceId,
			Long sessionId, String keyword, int page, int size) {
		NguoiDung actor = findActiveActor(actorEmail);
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw validation("Trang phải không âm và kích thước trang phải từ 1 đến 100.");
		}
		PageRequest pageable = PageRequest.of(page, size,
				Sort.by(Sort.Order.desc("reportedAt"), Sort.Order.desc("id")));
		return incidentRepository.search(actor.getId(), isManager(actor), status, severity,
				normalizeOptional(resourceId), sessionId, normalizeOptional(keyword), pageable).map(this::response);
	}

	@Transactional(readOnly = true)
	public IncidentResponse get(String actorEmail, String incidentId) {
		NguoiDung actor = findActiveActor(actorEmail);
		SuCo incident = incidentRepository.findDetailById(normalizeRequired(incidentId, "Mã sự cố không hợp lệ."))
				.orElseThrow(() -> notFound("Không tìm thấy sự cố."));
		assertCanView(actor, incident);
		return response(incident);
	}

	@Transactional
	public IncidentResponse update(String actorEmail, String incidentId, IncidentUpdateRequest request) {
		NguoiDung actor = findActiveActor(actorEmail);
		if (!isManager(actor)) {
			throw accessDenied("Chỉ cán bộ quản lý được xử lý sự cố.");
		}
		if (request == null || request.status() == null || request.version() == null) {
			throw validation("Trạng thái và version của sự cố không được để trống.");
		}
		SuCo incident = incidentRepository
				.findDetailByIdForUpdate(normalizeRequired(incidentId, "Mã sự cố không hợp lệ."))
				.orElseThrow(() -> notFound("Không tìm thấy sự cố."));
		if (incident.getVersion() != request.version()) {
			throw conflict("Sự cố đã được cập nhật bởi yêu cầu khác.");
		}
		NguoiDung handler = normalizeOptional(request.handlerId()) == null
				? incident.getHandler()
				: findActiveManager(request.handlerId());
		String result = request.result() == null ? incident.getResult() : normalizeOptional(request.result());
		if (result != null && result.length() > 2000) {
			throw validation("Kết quả xử lý không được vượt quá 2.000 ký tự.");
		}
		try {
			incident.updateHandling(handler, request.status(), result, clock.instant());
		} catch (IllegalStateException exception) {
			throw business(exception.getMessage());
		}
		incidentRepository.flush();
		return response(incident);
	}

	@Transactional(readOnly = true)
	public List<IncidentResourceOptionResponse> resourceOptions() {
		return resourceRepository.findAll(Sort.by("id")).stream().map(resource -> {
			if (resource.getResourceType() == LoaiTaiNguyen.PHONG) {
				return new IncidentResourceOptionResponse(resource.getId(), resource.getResourceType(),
						resource.getRoom().getId(), resource.getRoom().getName());
			}
			return new IncidentResourceOptionResponse(resource.getId(), resource.getResourceType(),
					resource.getDevice().getId(), resource.getDevice().getName());
		}).toList();
	}

	@Transactional(readOnly = true)
	public List<IncidentHandlerOptionResponse> handlerOptions() {
		return userRepository
				.findAllByRole_IdAndStatusOrderByFullNameAsc(RolePolicy.MANAGER, NguoiDungTrangThai.HOAT_DONG).stream()
				.map(user -> new IncidentHandlerOptionResponse(user.getId(), user.getFullName())).toList();
	}

	private void assertResourceBelongsToSession(TaiNguyen resource, PhienSuDung session) {
		PhieuDangKy registration = session.getSchedule().getRegistration();
		boolean belongs = resource.getResourceType() == LoaiTaiNguyen.PHONG
				? registration.getRoom().getId().equals(resource.getRoom().getId())
				: allocationRepository.existsByRegistration_IdAndDevice_IdAndAllocatedTrue(registration.getId(),
						resource.getDevice().getId());
		if (!belongs) {
			throw business("Tài nguyên không thuộc phòng hoặc danh sách thiết bị đã phân bổ của phiên.");
		}
	}

	private void assertSessionAccess(NguoiDung actor, PhienSuDung session) {
		PhieuDangKy registration = session.getSchedule().getRegistration();
		if (isManager(actor) || actor.getId().equals(registration.getCreator().getId())
				|| supervisionRepository.existsByRegistration_IdAndInstructor_Id(registration.getId(), actor.getId())) {
			return;
		}
		throw accessDenied("Bạn không có quyền gắn sự cố vào phiên sử dụng này.");
	}

	private void assertCanView(NguoiDung actor, SuCo incident) {
		if (isManager(actor) || actor.getId().equals(incident.getReporter().getId())) {
			return;
		}
		PhienSuDung session = incident.getSession();
		if (session != null) {
			PhieuDangKy registration = session.getSchedule().getRegistration();
			if (actor.getId().equals(registration.getCreator().getId()) || supervisionRepository
					.existsByRegistration_IdAndInstructor_Id(registration.getId(), actor.getId())) {
				return;
			}
		}
		throw accessDenied("Bạn không có quyền xem sự cố này.");
	}

	private PhienSuDung findSession(Long sessionId) {
		if (sessionId <= 0) {
			throw validation("Mã phiên sử dụng phải là số dương.");
		}
		return sessionRepository.findDetailById(sessionId).orElseThrow(() -> notFound("Không tìm thấy phiên sử dụng."));
	}

	private NguoiDung findActiveManager(String userId) {
		NguoiDung handler = userRepository.findById(normalizeRequired(userId, "Người xử lý không được để trống."))
				.orElseThrow(() -> notFound("Không tìm thấy người xử lý."));
		if (handler.getStatus() != NguoiDungTrangThai.HOAT_DONG
				|| !RolePolicy.MANAGER.equals(handler.getRole().getId())) {
			throw business("Người xử lý phải là cán bộ quản lý đang hoạt động.");
		}
		return handler;
	}

	private NguoiDung findActiveActor(String email) {
		String normalized = normalizeOptional(email);
		NguoiDung actor = normalized == null
				? null
				: userRepository.findByEmailIgnoreCase(normalized.toLowerCase(Locale.ROOT)).orElse(null);
		if (actor == null || actor.getStatus() != NguoiDungTrangThai.HOAT_DONG) {
			throw accessDenied("Tài khoản không hoạt động hoặc không tồn tại.");
		}
		return actor;
	}

	private IncidentResponse response(SuCo incident) {
		TaiNguyen resource = incident.getResource();
		String referenceId = resource.getResourceType() == LoaiTaiNguyen.PHONG
				? resource.getRoom().getId()
				: resource.getDevice().getId();
		String name = resource.getResourceType() == LoaiTaiNguyen.PHONG
				? resource.getRoom().getName()
				: resource.getDevice().getName();
		PhienSuDung session = incident.getSession();
		return new IncidentResponse(incident.getId(), incident.getVersion(), resource.getId(),
				resource.getResourceType(), referenceId, name, session == null ? null : session.getId(),
				session == null ? null : session.getSchedule().getRegistration().getId(),
				incident.getReporter().getId(), incident.getReporter().getFullName(),
				incident.getHandler() == null ? null : incident.getHandler().getId(),
				incident.getHandler() == null ? null : incident.getHandler().getFullName(), incident.getSeverity(),
				incident.getDescription(), incident.getStatus(), displayTime(incident.getReportedAt()),
				displayTime(incident.getCompletedAt()), incident.getResult());
	}

	private OffsetDateTime displayTime(java.time.Instant instant) {
		return instant == null ? null : instant.atZone(TimeConfiguration.DISPLAY_ZONE).toOffsetDateTime();
	}

	private boolean isManager(NguoiDung user) {
		return RolePolicy.isManager(user.getRole().getId());
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

	private ApiException validation(String message) {
		return new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, message);
	}

	private ApiException business(String message) {
		return new ApiException(ErrorCode.BUSINESS_RULE_VIOLATION, HttpStatus.UNPROCESSABLE_ENTITY, message);
	}

	private ApiException conflict(String message) {
		return new ApiException(ErrorCode.RESOURCE_CONFLICT, HttpStatus.CONFLICT, message);
	}

	private ApiException notFound(String message) {
		return new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, message);
	}

	private ApiException accessDenied(String message) {
		return new ApiException(ErrorCode.ACCESS_DENIED, HttpStatus.FORBIDDEN, message);
	}
}
