package com.example.labmanagement.registration.service;

import com.example.labmanagement.identity.domain.NguoiDung;
import com.example.labmanagement.registration.domain.HanhDongXuLyPhieu;
import com.example.labmanagement.registration.domain.LichDangKy;
import com.example.labmanagement.registration.domain.PhieuDangKy;
import com.example.labmanagement.registration.domain.PhieuDangKyThietBi;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import com.example.labmanagement.registration.domain.PhieuGiangDay;
import com.example.labmanagement.registration.domain.PhieuHuongDan;
import com.example.labmanagement.registration.domain.XuLyPhieu;
import com.example.labmanagement.registration.dto.RegistrationCancelRequest;
import com.example.labmanagement.registration.dto.RegistrationFormRequest;
import com.example.labmanagement.registration.dto.RegistrationResponse;
import com.example.labmanagement.registration.repository.LichDangKyRepository;
import com.example.labmanagement.registration.repository.PhieuDangKyRepository;
import com.example.labmanagement.registration.repository.PhieuDangKyThietBiRepository;
import com.example.labmanagement.registration.repository.PhieuGiangDayRepository;
import com.example.labmanagement.registration.repository.PhieuHuongDanRepository;
import com.example.labmanagement.registration.repository.XuLyPhieuRepository;
import com.example.labmanagement.usage.repository.PhienSuDungRepository;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RegistrationCommandService {

	private final PhieuDangKyRepository registrationRepository;
	private final LichDangKyRepository scheduleRepository;
	private final PhieuDangKyThietBiRepository allocationRepository;
	private final PhieuGiangDayRepository teachingRepository;
	private final PhieuHuongDanRepository supervisionRepository;
	private final XuLyPhieuRepository historyRepository;
	private final PhienSuDungRepository sessionRepository;
	private final RegistrationValidator validator;
	private final RegistrationQueryService queryService;
	private final EntityManager entityManager;
	private final Clock clock;

	RegistrationCommandService(PhieuDangKyRepository registrationRepository, LichDangKyRepository scheduleRepository,
			PhieuDangKyThietBiRepository allocationRepository, PhieuGiangDayRepository teachingRepository,
			PhieuHuongDanRepository supervisionRepository, XuLyPhieuRepository historyRepository,
			PhienSuDungRepository sessionRepository, RegistrationValidator validator,
			RegistrationQueryService queryService, EntityManager entityManager, Clock clock) {
		this.registrationRepository = registrationRepository;
		this.scheduleRepository = scheduleRepository;
		this.allocationRepository = allocationRepository;
		this.teachingRepository = teachingRepository;
		this.supervisionRepository = supervisionRepository;
		this.historyRepository = historyRepository;
		this.sessionRepository = sessionRepository;
		this.validator = validator;
		this.queryService = queryService;
		this.entityManager = entityManager;
		this.clock = clock;
	}

	@Transactional
	public RegistrationResponse create(String actorEmail, RegistrationFormRequest request) {
		NguoiDung actor = validator.findActiveActor(actorEmail);
		RegistrationValidator.PreparedRegistration prepared = validator.prepare(actor, request);
		PhieuDangKy registration = new PhieuDangKy(newRegistrationId(), actor, prepared.room(), request.type(),
				prepared.purpose(), prepared.participantCount(), request.startDate(), request.endDate(),
				PhieuDangKyTrangThai.CHO_DUYET);
		registration = registrationRepository.save(registration);
		createChildren(registration, prepared);
		registrationRepository.flush();
		return queryService.toDetail(registration, actor);
	}

	@Transactional
	public RegistrationResponse update(String actorEmail, String registrationId, RegistrationFormRequest request) {
		NguoiDung actor = validator.findActiveActor(actorEmail);
		PhieuDangKy registration = validator.findRegistration(registrationId);
		validator.assertOwner(actor, registration);
		if (registration.getStatus() != PhieuDangKyTrangThai.CHO_DUYET) {
			throw validator.conflict("Chỉ được sửa phiếu đang chờ duyệt.");
		}
		validator.assertVersion(registration, request.version());
		RegistrationValidator.PreparedRegistration prepared = validator.prepare(actor, request);
		registration.update(prepared.room(), request.type(), prepared.purpose(), prepared.participantCount(),
				request.startDate(), request.endDate(), clock.instant());
		deleteReplaceableChildren(registration.getId());
		entityManager.flush();
		createChildren(registration, prepared);
		entityManager.flush();
		entityManager.refresh(registration);
		return queryService.toDetail(registration, actor);
	}

	@Transactional
	public RegistrationResponse cancel(String actorEmail, String registrationId, RegistrationCancelRequest request) {
		NguoiDung actor = validator.findActiveActor(actorEmail);
		PhieuDangKy registration = validator.findRegistration(registrationId);
		validator.assertOwner(actor, registration);
		validator.assertVersion(registration, request.version());
		if (registration.getStatus() != PhieuDangKyTrangThai.CHO_DUYET
				&& registration.getStatus() != PhieuDangKyTrangThai.DA_DUYET) {
			throw validator.conflict("Trạng thái hiện tại không cho phép hủy phiếu.");
		}
		List<LichDangKy> schedules = scheduleRepository.findAllByRegistrationId(registration.getId());
		if (sessionRepository.existsStartedByRegistrationId(registration.getId())
				|| !queryService.isBeforeFirstSession(registration, schedules)) {
			throw validator.conflict("Không thể hủy phiếu sau khi phiên sử dụng đầu tiên đã bắt đầu.");
		}
		String reason = validator.normalizeRequired(request.reason(), "Lý do hủy không được để trống.");
		registration.cancel();
		historyRepository.save(new XuLyPhieu(registration, actor, HanhDongXuLyPhieu.HUY, reason, clock.instant()));
		registrationRepository.flush();
		return queryService.toDetail(registration, actor);
	}

	private void createChildren(PhieuDangKy registration, RegistrationValidator.PreparedRegistration prepared) {
		if (prepared.teaching() != null) {
			entityManager.persist(new PhieuGiangDay(registration, prepared.teaching().courseCode(),
					prepared.teaching().classGroup()));
		}
		if (prepared.supervisor() != null) {
			entityManager.persist(new PhieuHuongDan(registration, prepared.supervisor()));
		}
		scheduleRepository.saveAll(prepared.schedules().stream()
				.map(schedule -> new LichDangKy(registration, schedule.dayOfWeek(), schedule.period())).toList());
		for (var device : prepared.devices()) {
			entityManager.persist(new PhieuDangKyThietBi(registration, device, false));
		}
	}

	private void deleteReplaceableChildren(String registrationId) {
		teachingRepository.findByRegistrationId(registrationId).ifPresent(entityManager::remove);
		supervisionRepository.findByRegistrationId(registrationId).ifPresent(entityManager::remove);
		allocationRepository.findAllByRegistrationId(registrationId).forEach(entityManager::remove);
		scheduleRepository.findAllByRegistrationId(registrationId).forEach(entityManager::remove);
	}

	private String newRegistrationId() {
		return "PDK-" + UUID.randomUUID();
	}
}
