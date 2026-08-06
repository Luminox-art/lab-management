package com.example.labmanagement.usage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.labmanagement.catalog.domain.LoaiThietBi;
import com.example.labmanagement.catalog.domain.NhomPhong;
import com.example.labmanagement.catalog.domain.Phong;
import com.example.labmanagement.catalog.domain.PhongTrangThai;
import com.example.labmanagement.catalog.domain.TaiNguyen;
import com.example.labmanagement.catalog.domain.ThietBi;
import com.example.labmanagement.catalog.domain.ThietBiTrangThai;
import com.example.labmanagement.catalog.repository.TaiNguyenRepository;
import com.example.labmanagement.catalog.repository.ThietBiRepository;
import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.identity.domain.NguoiDung;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.domain.VaiTro;
import com.example.labmanagement.identity.repository.NguoiDungRepository;
import com.example.labmanagement.incident.domain.MucDoSuCo;
import com.example.labmanagement.incident.domain.SuCo;
import com.example.labmanagement.incident.repository.SuCoRepository;
import com.example.labmanagement.registration.domain.LichDangKy;
import com.example.labmanagement.registration.domain.LoaiPhieu;
import com.example.labmanagement.registration.domain.PhieuDangKy;
import com.example.labmanagement.registration.domain.PhieuDangKyThietBi;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import com.example.labmanagement.registration.repository.PhieuDangKyThietBiRepository;
import com.example.labmanagement.scheduling.domain.TietHoc;
import com.example.labmanagement.usage.domain.PhienSuDung;
import com.example.labmanagement.usage.domain.PhienSuDungThietBi;
import com.example.labmanagement.usage.domain.PhienSuDungTrangThai;
import com.example.labmanagement.usage.dto.SessionCheckInRequest;
import com.example.labmanagement.usage.dto.SessionCheckOutRequest;
import com.example.labmanagement.usage.dto.SessionDeviceConditionRequest;
import com.example.labmanagement.usage.dto.SessionIncidentRequest;
import com.example.labmanagement.usage.dto.UsageSessionResponse;
import com.example.labmanagement.usage.repository.PhienSuDungRepository;
import com.example.labmanagement.usage.repository.PhienSuDungThietBiRepository;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class UsageSessionServiceTest {

	private static final Instant NOW = Instant.parse("2026-08-01T01:00:00Z");
	private static final String EMAIL = "gv-use@lab.local";

	@Mock
	private NguoiDungRepository userRepository;
	@Mock
	private PhienSuDungRepository sessionRepository;
	@Mock
	private PhienSuDungThietBiRepository sessionDeviceRepository;
	@Mock
	private PhieuDangKyThietBiRepository allocationRepository;
	@Mock
	private ThietBiRepository deviceRepository;
	@Mock
	private TaiNguyenRepository resourceRepository;
	@Mock
	private SuCoRepository incidentRepository;
	@Mock
	private EntityManager entityManager;

	private UsageSessionService service;
	private NguoiDung actor;
	private PhieuDangKy registration;
	private PhienSuDung session;
	private ThietBi device;
	private PhieuDangKyThietBi allocation;

	@BeforeEach
	void setUp() {
		service = new UsageSessionService(userRepository, sessionRepository, sessionDeviceRepository,
				allocationRepository, deviceRepository, resourceRepository, incidentRepository, entityManager,
				Clock.fixed(NOW, ZoneOffset.UTC));
		actor = new NguoiDung("GV-USE", "Giảng viên", EMAIL, "hash", null, new VaiTro("GV", "Giảng viên"),
				NguoiDungTrangThai.HOAT_DONG);
		Phong room = new Phong("P-USE", "Phòng dùng", new NhomPhong("N-USE", "Nhóm", null), "A1", 20,
				PhongTrangThai.SAN_SANG);
		registration = new PhieuDangKy("PDK-USE", actor, room, LoaiPhieu.GIANG_DAY, "Thực hành", 10,
				LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 1), PhieuDangKyTrangThai.DA_DUYET);
		TietHoc period = new TietHoc(1, "Tiết 1", LocalTime.of(7, 30), LocalTime.of(9, 0));
		session = new PhienSuDung(new LichDangKy(registration, 7, period), LocalDate.of(2026, 8, 1),
				PhienSuDungTrangThai.CHUA_BAT_DAU, null, null, null, null);
		LoaiThietBi type = new LoaiThietBi("LT-USE", "Máy đo", false, true, null);
		device = new ThietBi("TB-USE", "Máy đo 01", type, "SN-USE", "M1", room, ThietBiTrangThai.SAN_SANG);
		allocation = new PhieuDangKyThietBi(registration, device, true);
		when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(actor));
		when(sessionRepository.findDetailByIdForUpdate(1L)).thenReturn(Optional.of(session));
	}

	@Test
	void checkInRecordsDeviceAndMovesSessionRegistrationAndDeviceTogether() {
		when(allocationRepository.findAllocatedByRegistrationId(registration.getId())).thenReturn(List.of(allocation));
		when(deviceRepository.findAllByIdForUpdateOrderById(anyCollection())).thenReturn(List.of(device));
		when(sessionDeviceRepository.findAllBySessionId(session.getId())).thenReturn(List.of());

		UsageSessionResponse response = service.checkIn(EMAIL, 1L,
				new SessionCheckInRequest(0L, List.of(new SessionDeviceConditionRequest(device.getId(), "Tốt", null))));

		assertThat(response.status()).isEqualTo(PhienSuDungTrangThai.DANG_SU_DUNG);
		assertThat(registration.getStatus()).isEqualTo(PhieuDangKyTrangThai.DANG_SU_DUNG);
		assertThat(device.getStatus()).isEqualTo(ThietBiTrangThai.DANG_SU_DUNG);
		verify(sessionDeviceRepository).saveAll(org.mockito.ArgumentMatchers.anyList());
	}

	@Test
	void rejectsSecondCheckInAfterSessionStateChanged() {
		session.checkIn(actor, NOW.minusSeconds(60));

		assertThatThrownBy(() -> service.checkIn(EMAIL, 1L, new SessionCheckInRequest(0L, List.of())))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));
	}

	@Test
	void checkOutCreatesIncidentAndCompletesLastSessionAtomically() {
		session.checkIn(actor, NOW.minusSeconds(60));
		registration.startUsing(NOW.minusSeconds(60));
		device.startUsing();
		PhienSuDungThietBi sessionDevice = new PhienSuDungThietBi(session, device, "Tốt", null, null);
		when(sessionDeviceRepository.findAllBySessionIdForUpdate(session.getId())).thenReturn(List.of(sessionDevice));
		when(sessionDeviceRepository.findAllBySessionId(session.getId())).thenReturn(List.of(sessionDevice));
		when(deviceRepository.findAllByIdForUpdateOrderById(anyCollection())).thenReturn(List.of(device));
		when(resourceRepository.findAllByDevice_IdIn(anyCollection()))
				.thenReturn(List.of(TaiNguyen.forDevice("TN-USE", device)));

		UsageSessionResponse response = service.checkOut(EMAIL, 1L,
				new SessionCheckOutRequest(0L,
						List.of(new SessionDeviceConditionRequest(device.getId(), "Hỏng đầu đo", "Không lên nguồn")),
						List.of(new SessionIncidentRequest(device.getId(), MucDoSuCo.CAO, "Thiết bị mất nguồn"))));

		assertThat(response.status()).isEqualTo(PhienSuDungTrangThai.HOAN_THANH);
		assertThat(registration.getStatus()).isEqualTo(PhieuDangKyTrangThai.HOAN_THANH);
		assertThat(device.getStatus()).isEqualTo(ThietBiTrangThai.HONG);
		assertThat(sessionDevice.getReturnedCondition()).isEqualTo("Hỏng đầu đo");
		assertThat(response.incidentIds()).hasSize(1);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<SuCo>> incidents = ArgumentCaptor.forClass(List.class);
		verify(incidentRepository).saveAll(incidents.capture());
		assertThat(incidents.getValue()).singleElement().extracting(SuCo::getSeverity).isEqualTo(MucDoSuCo.CAO);
	}
}
