package com.example.labmanagement.maintenance.service;

import com.example.labmanagement.catalog.domain.LoaiTaiNguyen;
import com.example.labmanagement.catalog.domain.PhongTrangThai;
import com.example.labmanagement.catalog.domain.TaiNguyen;
import com.example.labmanagement.catalog.domain.ThietBiTrangThai;
import com.example.labmanagement.catalog.repository.TaiNguyenRepository;
import com.example.labmanagement.common.clock.TimeConfiguration;
import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.common.error.ErrorCode;
import com.example.labmanagement.identity.domain.NguoiDung;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.domain.RolePolicy;
import com.example.labmanagement.identity.repository.NguoiDungRepository;
import com.example.labmanagement.incident.domain.SuCo;
import com.example.labmanagement.incident.repository.SuCoRepository;
import com.example.labmanagement.maintenance.domain.BaoTri;
import com.example.labmanagement.maintenance.domain.BaoTriSuCo;
import com.example.labmanagement.maintenance.domain.BaoTriTrangThai;
import com.example.labmanagement.maintenance.domain.TienDoBaoTri;
import com.example.labmanagement.maintenance.dto.MaintenanceAssigneeOptionResponse;
import com.example.labmanagement.maintenance.dto.MaintenanceCreateRequest;
import com.example.labmanagement.maintenance.dto.MaintenanceProgressResponse;
import com.example.labmanagement.maintenance.dto.MaintenanceResourceOptionResponse;
import com.example.labmanagement.maintenance.dto.MaintenanceResponse;
import com.example.labmanagement.maintenance.dto.MaintenanceUpdateRequest;
import com.example.labmanagement.maintenance.repository.BaoTriRepository;
import com.example.labmanagement.maintenance.repository.BaoTriSuCoRepository;
import com.example.labmanagement.maintenance.repository.TienDoBaoTriRepository;
import com.example.labmanagement.scheduling.domain.LichChan;
import com.example.labmanagement.scheduling.domain.LichChanTrangThai;
import com.example.labmanagement.scheduling.repository.LichChanRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaintenanceService {

	private static final int MAX_PAGE_SIZE = 100;
	private static final LocalDate OPEN_BLOCK_END = LocalDate.of(9999, 12, 31);
	private static final Set<BaoTriTrangThai> ACTIVE_STATUSES = Set.of(BaoTriTrangThai.CHO_XU_LY,
			BaoTriTrangThai.DANG_BAO_TRI);

	private final NguoiDungRepository userRepository;
	private final TaiNguyenRepository resourceRepository;
	private final SuCoRepository incidentRepository;
	private final BaoTriRepository maintenanceRepository;
	private final BaoTriSuCoRepository maintenanceIncidentRepository;
	private final TienDoBaoTriRepository progressRepository;
	private final LichChanRepository blockedScheduleRepository;
	private final Clock clock;

	public MaintenanceService(NguoiDungRepository userRepository, TaiNguyenRepository resourceRepository,
			SuCoRepository incidentRepository, BaoTriRepository maintenanceRepository,
			BaoTriSuCoRepository maintenanceIncidentRepository, TienDoBaoTriRepository progressRepository,
			LichChanRepository blockedScheduleRepository, Clock clock) {
		this.userRepository = userRepository;
		this.resourceRepository = resourceRepository;
		this.incidentRepository = incidentRepository;
		this.maintenanceRepository = maintenanceRepository;
		this.maintenanceIncidentRepository = maintenanceIncidentRepository;
		this.progressRepository = progressRepository;
		this.blockedScheduleRepository = blockedScheduleRepository;
		this.clock = clock;
	}

	@Transactional
	public MaintenanceResponse create(String actorEmail, MaintenanceCreateRequest request) {
		NguoiDung actor = findManagerByEmail(actorEmail);
		if (request == null) {
			throw validation("Thông tin bảo trì không được để trống.");
		}
		String resourceId = normalizeRequired(request.resourceId(), "Tài nguyên không được để trống.");
		String content = normalizeRequired(request.content(), "Nội dung bảo trì không được để trống.");
		assertLength(content, 2000, "Nội dung bảo trì không được vượt quá 2.000 ký tự.");
		TaiNguyen resource = resourceRepository.findByIdForUpdate(resourceId)
				.orElseThrow(() -> notFound("Không tìm thấy tài nguyên."));
		if (maintenanceRepository.existsByResource_IdAndStatusIn(resourceId, ACTIVE_STATUSES)) {
			throw conflict("Tài nguyên đã có bảo trì đang hoạt động.");
		}
		assertCanStartMaintenance(resource);
		NguoiDung assignee = findManagerById(request.assigneeId());
		SuCo incident = findSourceIncident(request.incidentId(), resource);

		Instant now = clock.instant();
		String id = "BT-" + UUID.randomUUID();
		BaoTri maintenance = maintenanceRepository
				.save(new BaoTri(id, resource, assignee, now, null, content, BaoTriTrangThai.CHO_XU_LY, null));
		if (incident != null) {
			maintenanceIncidentRepository.save(new BaoTriSuCo(maintenance, incident));
		}
		progressRepository.save(new TienDoBaoTri(maintenance, now, BaoTriTrangThai.CHO_XU_LY,
				"Tiếp nhận yêu cầu bảo trì: " + content, actor));
		ZonedDateTime displayNow = ZonedDateTime.now(clock.withZone(TimeConfiguration.DISPLAY_ZONE));
		blockedScheduleRepository.save(new LichChan(resource, displayNow.toLocalDate(), OPEN_BLOCK_END, null, null,
				"Bảo trì " + id + ": " + content, LichChanTrangThai.HIEU_LUC, actor, id));
		startResourceMaintenance(resource);
		maintenanceRepository.flush();
		return response(maintenance);
	}

	@Transactional
	public MaintenanceResponse update(String actorEmail, String maintenanceId, MaintenanceUpdateRequest request) {
		NguoiDung actor = findManagerByEmail(actorEmail);
		if (request == null || request.status() == null || request.version() == null) {
			throw validation("Trạng thái và version bảo trì không được để trống.");
		}
		String progressContent = normalizeRequired(request.progressContent(), "Nội dung tiến độ không được để trống.");
		assertLength(progressContent, 2000, "Nội dung tiến độ không được vượt quá 2.000 ký tự.");
		String result = normalizeOptional(request.result());
		assertLength(result, 2000, "Kết quả bảo trì không được vượt quá 2.000 ký tự.");
		BaoTri maintenance = maintenanceRepository
				.findDetailByIdForUpdate(normalizeRequired(maintenanceId, "Mã bảo trì không hợp lệ."))
				.orElseThrow(() -> notFound("Không tìm thấy bảo trì."));
		if (maintenance.getVersion() != request.version()) {
			throw conflict("Bảo trì đã được cập nhật bởi yêu cầu khác.");
		}
		try {
			maintenance.updateProgress(request.status(), request.endAt(), result);
		} catch (IllegalStateException exception) {
			throw business(exception.getMessage());
		}
		Instant now = clock.instant();
		progressRepository.save(new TienDoBaoTri(maintenance, now, request.status(), progressContent, actor));
		if (request.status() == BaoTriTrangThai.HOAN_THANH || request.status() == BaoTriTrangThai.DA_HUY) {
			finishResourceAndBlock(maintenance);
		}
		maintenanceRepository.flush();
		return response(maintenance);
	}

	@Transactional(readOnly = true)
	public Page<MaintenanceResponse> search(String actorEmail, BaoTriTrangThai status, String resourceId,
			String assigneeId, String keyword, int page, int size) {
		findManagerByEmail(actorEmail);
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw validation("Trang phải không âm và kích thước trang phải từ 1 đến 100.");
		}
		PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("startAt"), Sort.Order.desc("id")));
		return maintenanceRepository.search(status, normalizeOptional(resourceId), normalizeOptional(assigneeId),
				normalizeOptional(keyword), pageable).map(this::response);
	}

	@Transactional(readOnly = true)
	public MaintenanceResponse get(String actorEmail, String maintenanceId) {
		findManagerByEmail(actorEmail);
		BaoTri maintenance = maintenanceRepository
				.findDetailById(normalizeRequired(maintenanceId, "Mã bảo trì không hợp lệ."))
				.orElseThrow(() -> notFound("Không tìm thấy bảo trì."));
		return response(maintenance);
	}

	@Transactional(readOnly = true)
	public List<MaintenanceResourceOptionResponse> resourceOptions() {
		return resourceRepository.findAll(Sort.by("id")).stream().map(resource -> {
			if (resource.getResourceType() == LoaiTaiNguyen.PHONG) {
				return new MaintenanceResourceOptionResponse(resource.getId(), resource.getResourceType(),
						resource.getRoom().getId(), resource.getRoom().getName());
			}
			return new MaintenanceResourceOptionResponse(resource.getId(), resource.getResourceType(),
					resource.getDevice().getId(), resource.getDevice().getName());
		}).toList();
	}

	@Transactional(readOnly = true)
	public List<MaintenanceAssigneeOptionResponse> assigneeOptions() {
		return userRepository
				.findAllByRole_IdAndStatusOrderByFullNameAsc(RolePolicy.MANAGER, NguoiDungTrangThai.HOAT_DONG).stream()
				.map(user -> new MaintenanceAssigneeOptionResponse(user.getId(), user.getFullName())).toList();
	}

	private SuCo findSourceIncident(String incidentId, TaiNguyen resource) {
		String normalized = normalizeOptional(incidentId);
		if (normalized == null) {
			return null;
		}
		SuCo incident = incidentRepository.findDetailByIdForUpdate(normalized)
				.orElseThrow(() -> notFound("Không tìm thấy sự cố nguồn."));
		if (!incident.getResource().getId().equals(resource.getId())) {
			throw business("Sự cố nguồn và bảo trì phải cùng tài nguyên.");
		}
		if (maintenanceIncidentRepository.existsByIncident_Id(incident.getId())) {
			throw conflict("Sự cố đã được dùng làm nguồn cho một bảo trì khác.");
		}
		return incident;
	}

	private void assertCanStartMaintenance(TaiNguyen resource) {
		if (resource.getResourceType() == LoaiTaiNguyen.PHONG) {
			if (resource.getRoom().getStatus() != PhongTrangThai.SAN_SANG) {
				throw conflict("Phòng không ở trạng thái sẵn sàng để bắt đầu bảo trì.");
			}
			return;
		}
		ThietBiTrangThai status = resource.getDevice().getStatus();
		if (status != ThietBiTrangThai.SAN_SANG && status != ThietBiTrangThai.HONG) {
			throw conflict("Thiết bị không ở trạng thái sẵn sàng hoặc hỏng để bắt đầu bảo trì.");
		}
	}

	private void startResourceMaintenance(TaiNguyen resource) {
		if (resource.getResourceType() == LoaiTaiNguyen.PHONG) {
			resource.getRoom().startMaintenance();
		} else {
			resource.getDevice().startMaintenance();
		}
	}

	private void finishResourceAndBlock(BaoTri maintenance) {
		TaiNguyen resource = resourceRepository.findByIdForUpdate(maintenance.getResource().getId())
				.orElseThrow(() -> notFound("Không tìm thấy tài nguyên bảo trì."));
		LichChan blocked = blockedScheduleRepository.findByMaintenanceIdForUpdate(maintenance.getId())
				.orElseThrow(() -> conflict("Không tìm thấy lịch chặn liên kết với bảo trì."));
		if (blocked.getStatus() != LichChanTrangThai.HIEU_LUC) {
			throw conflict("Lịch chặn liên kết với bảo trì không còn hiệu lực.");
		}
		blocked.cancel();
		boolean completed = maintenance.getStatus() == BaoTriTrangThai.HOAN_THANH;
		if (resource.getResourceType() == LoaiTaiNguyen.PHONG) {
			if (completed) {
				resource.getRoom().finishMaintenance();
			} else {
				resource.getRoom().cancelMaintenance();
			}
		} else {
			if (completed) {
				resource.getDevice().finishMaintenance();
			} else {
				resource.getDevice().cancelMaintenance();
			}
		}
	}

	private MaintenanceResponse response(BaoTri maintenance) {
		TaiNguyen resource = maintenance.getResource();
		String referenceId = resource.getResourceType() == LoaiTaiNguyen.PHONG
				? resource.getRoom().getId()
				: resource.getDevice().getId();
		String resourceName = resource.getResourceType() == LoaiTaiNguyen.PHONG
				? resource.getRoom().getName()
				: resource.getDevice().getName();
		String incidentId = maintenanceIncidentRepository.findByMaintenance_Id(maintenance.getId())
				.map(link -> link.getIncident().getId()).orElse(null);
		List<MaintenanceProgressResponse> progress = progressRepository
				.findAllByMaintenance_IdOrderByOccurredAtAscIdAsc(maintenance.getId()).stream()
				.map(item -> new MaintenanceProgressResponse(item.getId(), displayTime(item.getOccurredAt()),
						item.getStatus(), item.getContent(), item.getUpdatedBy().getId(),
						item.getUpdatedBy().getFullName()))
				.toList();
		return new MaintenanceResponse(maintenance.getId(), maintenance.getVersion(), resource.getId(),
				resource.getResourceType(), referenceId, resourceName, incidentId, maintenance.getAssignee().getId(),
				maintenance.getAssignee().getFullName(), displayTime(maintenance.getStartAt()),
				displayTime(maintenance.getEndAt()), maintenance.getContent(), maintenance.getStatus(),
				maintenance.getResult(), progress);
	}

	private NguoiDung findManagerByEmail(String email) {
		String normalized = normalizeOptional(email);
		NguoiDung manager = normalized == null
				? null
				: userRepository.findByEmailIgnoreCase(normalized.toLowerCase(Locale.ROOT)).orElse(null);
		if (!isActiveManagerActor(manager)) {
			throw accessDenied("Chỉ cán bộ quản lý đang hoạt động được quản lý bảo trì.");
		}
		return manager;
	}

	private NguoiDung findManagerById(String id) {
		NguoiDung manager = userRepository.findById(normalizeRequired(id, "Người phụ trách không được để trống."))
				.orElseThrow(() -> notFound("Không tìm thấy người phụ trách."));
		if (!isActiveAssignee(manager)) {
			throw business("Người phụ trách phải là cán bộ quản lý đang hoạt động.");
		}
		return manager;
	}

	private boolean isActiveManagerActor(NguoiDung user) {
		return user != null && user.getStatus() == NguoiDungTrangThai.HOAT_DONG
				&& RolePolicy.isManager(user.getRole().getId());
	}

	private boolean isActiveAssignee(NguoiDung user) {
		return user != null && user.getStatus() == NguoiDungTrangThai.HOAT_DONG
				&& RolePolicy.MANAGER.equals(user.getRole().getId());
	}

	private OffsetDateTime displayTime(Instant instant) {
		return instant == null ? null : instant.atZone(TimeConfiguration.DISPLAY_ZONE).toOffsetDateTime();
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

	private void assertLength(String value, int max, String message) {
		if (value != null && value.length() > max) {
			throw validation(message);
		}
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
