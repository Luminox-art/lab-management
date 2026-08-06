package com.example.labmanagement.incident.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.labmanagement.catalog.domain.LoaiThietBi;
import com.example.labmanagement.catalog.domain.NhomPhong;
import com.example.labmanagement.catalog.domain.Phong;
import com.example.labmanagement.catalog.domain.PhongTrangThai;
import com.example.labmanagement.catalog.domain.TaiNguyen;
import com.example.labmanagement.catalog.domain.ThietBi;
import com.example.labmanagement.catalog.domain.ThietBiTrangThai;
import com.example.labmanagement.catalog.repository.TaiNguyenRepository;
import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.identity.domain.NguoiDung;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.domain.VaiTro;
import com.example.labmanagement.identity.repository.NguoiDungRepository;
import com.example.labmanagement.incident.domain.MucDoSuCo;
import com.example.labmanagement.incident.domain.SuCo;
import com.example.labmanagement.incident.domain.SuCoTrangThai;
import com.example.labmanagement.incident.dto.IncidentCreateRequest;
import com.example.labmanagement.incident.dto.IncidentResponse;
import com.example.labmanagement.incident.dto.IncidentUpdateRequest;
import com.example.labmanagement.incident.repository.SuCoRepository;
import com.example.labmanagement.registration.domain.LichDangKy;
import com.example.labmanagement.registration.domain.LoaiPhieu;
import com.example.labmanagement.registration.domain.PhieuDangKy;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import com.example.labmanagement.registration.repository.PhieuDangKyThietBiRepository;
import com.example.labmanagement.registration.repository.PhieuHuongDanRepository;
import com.example.labmanagement.scheduling.domain.TietHoc;
import com.example.labmanagement.usage.domain.PhienSuDung;
import com.example.labmanagement.usage.domain.PhienSuDungTrangThai;
import com.example.labmanagement.usage.repository.PhienSuDungRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

	private static final Instant NOW = Instant.parse("2026-08-01T02:00:00Z");
	private static final String REPORTER_EMAIL = "gv-inc@lab.local";
	private static final String MANAGER_EMAIL = "cb-inc@lab.local";

	@Mock
	private NguoiDungRepository userRepository;
	@Mock
	private TaiNguyenRepository resourceRepository;
	@Mock
	private PhienSuDungRepository sessionRepository;
	@Mock
	private PhieuDangKyThietBiRepository allocationRepository;
	@Mock
	private PhieuHuongDanRepository supervisionRepository;
	@Mock
	private SuCoRepository incidentRepository;

	private IncidentService service;
	private NguoiDung reporter;
	private NguoiDung manager;
	private PhienSuDung session;
	private TaiNguyen deviceResource;

	@BeforeEach
	void setUp() {
		service = new IncidentService(userRepository, resourceRepository, sessionRepository, allocationRepository,
				supervisionRepository, incidentRepository, Clock.fixed(NOW, ZoneOffset.UTC));
		reporter = new NguoiDung("GV-INC", "Giảng viên", REPORTER_EMAIL, "hash", null, new VaiTro("GV", "Giảng viên"),
				NguoiDungTrangThai.HOAT_DONG);
		manager = new NguoiDung("CB-INC", "Cán bộ", MANAGER_EMAIL, "hash", null, new VaiTro("CBQL", "Cán bộ quản lý"),
				NguoiDungTrangThai.HOAT_DONG);
		Phong room = new Phong("P-INC", "Phòng sự cố", new NhomPhong("N-INC", "Nhóm", null), "A1", 20,
				PhongTrangThai.SAN_SANG);
		PhieuDangKy registration = new PhieuDangKy("PDK-INC", reporter, room, LoaiPhieu.GIANG_DAY, "Thực hành", 10,
				LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 1), PhieuDangKyTrangThai.HOAN_THANH);
		session = new PhienSuDung(
				new LichDangKy(registration, 7, new TietHoc(1, "Tiết 1", LocalTime.of(7, 0), LocalTime.of(7, 50))),
				LocalDate.of(2026, 8, 1), PhienSuDungTrangThai.HOAN_THANH, NOW.minusSeconds(3600), NOW.minusSeconds(60),
				reporter, reporter);
		ThietBi device = new ThietBi("TB-INC", "Máy đo", new LoaiThietBi("LT-INC", "Máy đo", false, true, null),
				"SN-INC", "M1", room, ThietBiTrangThai.SAN_SANG);
		deviceResource = TaiNguyen.forDevice("TN-INC", device);
	}

	@Test
	void reportAcceptsAllocatedDeviceAndMarksItBroken() {
		when(userRepository.findByEmailIgnoreCase(REPORTER_EMAIL)).thenReturn(Optional.of(reporter));
		when(resourceRepository.findById("TN-INC")).thenReturn(Optional.of(deviceResource));
		when(sessionRepository.findDetailById(9L)).thenReturn(Optional.of(session));
		when(allocationRepository.existsByRegistration_IdAndDevice_IdAndAllocatedTrue("PDK-INC", "TB-INC"))
				.thenReturn(true);
		when(incidentRepository.save(org.mockito.ArgumentMatchers.any(SuCo.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		IncidentResponse result = service.report(REPORTER_EMAIL,
				new IncidentCreateRequest(" TN-INC ", 9L, MucDoSuCo.CAO, " Mất nguồn "));

		assertThat(result.status()).isEqualTo(SuCoTrangThai.MOI);
		assertThat(result.reporterId()).isEqualTo("GV-INC");
		assertThat(deviceResource.getDevice().getStatus()).isEqualTo(ThietBiTrangThai.HONG);
	}

	@Test
	void reportRejectsADeviceOutsideTheSessionAllocation() {
		when(userRepository.findByEmailIgnoreCase(REPORTER_EMAIL)).thenReturn(Optional.of(reporter));
		when(resourceRepository.findById("TN-INC")).thenReturn(Optional.of(deviceResource));
		when(sessionRepository.findDetailById(9L)).thenReturn(Optional.of(session));

		assertThatThrownBy(() -> service.report(REPORTER_EMAIL,
				new IncidentCreateRequest("TN-INC", 9L, MucDoSuCo.CAO, "Mất nguồn")))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
	}

	@Test
	void completionRequiresHandlerAndResultThenRecordsServerTime() {
		when(userRepository.findByEmailIgnoreCase(MANAGER_EMAIL)).thenReturn(Optional.of(manager));
		when(userRepository.findById("CB-INC")).thenReturn(Optional.of(manager));
		SuCo incident = new SuCo("SC-INC", deviceResource, null, reporter, null, MucDoSuCo.CAO, "Mất nguồn",
				SuCoTrangThai.DANG_XU_LY, NOW.minusSeconds(60), null, null);
		when(incidentRepository.findDetailByIdForUpdate("SC-INC")).thenReturn(Optional.of(incident));

		assertThatThrownBy(() -> service.update(MANAGER_EMAIL, "SC-INC",
				new IncidentUpdateRequest("CB-INC", SuCoTrangThai.DA_XU_LY, " ", 0L)))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

		IncidentResponse result = service.update(MANAGER_EMAIL, "SC-INC",
				new IncidentUpdateRequest("CB-INC", SuCoTrangThai.DA_XU_LY, "Đã thay nguồn", 0L));
		assertThat(result.status()).isEqualTo(SuCoTrangThai.DA_XU_LY);
		assertThat(result.completedAt().toInstant()).isEqualTo(NOW);
		assertThat(result.handlerId()).isEqualTo("CB-INC");
	}
}
