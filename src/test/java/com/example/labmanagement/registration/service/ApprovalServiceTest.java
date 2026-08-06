package com.example.labmanagement.registration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.labmanagement.catalog.domain.LoaiThietBi;
import com.example.labmanagement.catalog.domain.NhomPhong;
import com.example.labmanagement.catalog.domain.Phong;
import com.example.labmanagement.catalog.domain.PhongTrangThai;
import com.example.labmanagement.catalog.domain.TaiNguyen;
import com.example.labmanagement.catalog.domain.ThietBi;
import com.example.labmanagement.catalog.domain.ThietBiTrangThai;
import com.example.labmanagement.catalog.repository.PhongRepository;
import com.example.labmanagement.catalog.repository.TaiNguyenRepository;
import com.example.labmanagement.catalog.repository.ThietBiRepository;
import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.common.error.ErrorCode;
import com.example.labmanagement.identity.domain.NguoiDung;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.domain.VaiTro;
import com.example.labmanagement.identity.repository.NguoiDungRepository;
import com.example.labmanagement.registration.domain.HanhDongXuLyPhieu;
import com.example.labmanagement.registration.domain.LichDangKy;
import com.example.labmanagement.registration.domain.LoaiPhieu;
import com.example.labmanagement.registration.domain.PhieuDangKy;
import com.example.labmanagement.registration.domain.PhieuDangKyThietBi;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import com.example.labmanagement.registration.domain.XuLyPhieu;
import com.example.labmanagement.registration.dto.ApprovalRequest;
import com.example.labmanagement.registration.dto.RegistrationDecisionResponse;
import com.example.labmanagement.registration.dto.RejectionRequest;
import com.example.labmanagement.registration.repository.LichDangKyRepository;
import com.example.labmanagement.registration.repository.PhieuDangKyRepository;
import com.example.labmanagement.registration.repository.PhieuDangKyThietBiRepository;
import com.example.labmanagement.registration.repository.PhieuHuongDanRepository;
import com.example.labmanagement.registration.repository.XuLyPhieuRepository;
import com.example.labmanagement.scheduling.domain.TietHoc;
import com.example.labmanagement.scheduling.dto.AvailabilityResponse;
import com.example.labmanagement.scheduling.dto.PeriodResponse;
import com.example.labmanagement.scheduling.service.SchedulingService;
import com.example.labmanagement.usage.service.SessionGenerationService;
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

/** TC-APR-10..20 service rules for API-22/23. */
@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

	private static final Instant NOW = Instant.parse("2026-08-01T01:00:00Z");
	private static final String MANAGER_EMAIL = "manager@lab.local";
	private static final LocalDate USE_DATE = LocalDate.of(2026, 9, 7);

	@Mock
	private NguoiDungRepository userRepository;
	@Mock
	private PhieuDangKyRepository registrationRepository;
	@Mock
	private PhongRepository roomRepository;
	@Mock
	private ThietBiRepository deviceRepository;
	@Mock
	private TaiNguyenRepository resourceRepository;
	@Mock
	private LichDangKyRepository scheduleRepository;
	@Mock
	private PhieuDangKyThietBiRepository allocationRepository;
	@Mock
	private PhieuHuongDanRepository supervisionRepository;
	@Mock
	private XuLyPhieuRepository historyRepository;
	@Mock
	private SchedulingService schedulingService;
	@Mock
	private SessionGenerationService sessionGenerationService;
	@Mock
	private EntityManager entityManager;

	private ApprovalService service;
	private NguoiDung manager;
	private NguoiDung creator;
	private Phong room;
	private ThietBi device;
	private PhieuDangKy registration;

	@BeforeEach
	void setUp() {
		service = new ApprovalService(userRepository, registrationRepository, roomRepository, deviceRepository,
				resourceRepository, scheduleRepository, allocationRepository, supervisionRepository, historyRepository,
				schedulingService, sessionGenerationService, entityManager, Clock.fixed(NOW, ZoneOffset.UTC));
		manager = user("CB-TEST", MANAGER_EMAIL, "CBQL");
		creator = user("GV-TEST", "teacher@lab.local", "GV");
		room = room(30);
		device = device("TB-01", false, true, null);
		registration = registration(creator, room, 20);
		when(userRepository.findByEmailIgnoreCase(MANAGER_EMAIL)).thenReturn(Optional.of(manager));
	}

	@Test
	void approvesPendingRegistrationAndPersistsExactlyOneDecisionHistory() {
		stubApproval(registration, List.of(device));

		RegistrationDecisionResponse response = service.approve(MANAGER_EMAIL, registration.getId(),
				new ApprovalRequest(List.of(device.getId()), 0L));

		assertThat(response.status()).isEqualTo(PhieuDangKyTrangThai.DA_DUYET);
		assertThat(response.allocatedDeviceIds()).containsExactly(device.getId());
		assertThat(allocationRepository.findAllByRegistrationId(registration.getId())).isNotNull();
		ArgumentCaptor<XuLyPhieu> history = ArgumentCaptor.forClass(XuLyPhieu.class);
		verify(historyRepository).save(history.capture());
		assertThat(history.getValue().getAction()).isEqualTo(HanhDongXuLyPhieu.PHE_DUYET);
		assertThat(history.getValue().getReason()).isNull();
		verify(registrationRepository).flush();
	}

	@Test
	void rejectsCapacityViolationWithoutChangingAggregate() {
		registration = registration(creator, room(5), 20);
		stubApproval(registration, List.of());

		assertThatThrownBy(
				() -> service.approve(MANAGER_EMAIL, registration.getId(), new ApprovalRequest(List.of(), 0L)))
				.isInstanceOfSatisfying(ApiException.class, exception -> {
					assertThat(exception.getCode()).isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
					assertThat(exception.getStatus().value()).isEqualTo(422);
				});

		assertThat(registration.getStatus()).isEqualTo(PhieuDangKyTrangThai.CHO_DUYET);
		verify(historyRepository, never()).save(any());
	}

	@Test
	void controlledStudentDeviceRequiresSupervisor() {
		creator = user("SV-TEST", "student@lab.local", "SV");
		device = device("TB-ROBOT", true, true, null);
		registration = registration(creator, room, 10);
		stubApproval(registration, List.of(device));
		when(supervisionRepository.findByRegistrationId(registration.getId())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.approve(MANAGER_EMAIL, registration.getId(),
				new ApprovalRequest(List.of(device.getId()), 0L))).isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getStatus().value()).isEqualTo(422));
		verify(historyRepository, never()).save(any());
	}

	@Test
	void staleOrAlreadyDecidedRegistrationCannotBeProcessedAgain() {
		when(registrationRepository.findDetailByIdForUpdate(registration.getId()))
				.thenReturn(Optional.of(registration));

		assertThatThrownBy(
				() -> service.approve(MANAGER_EMAIL, registration.getId(), new ApprovalRequest(List.of(), 1L)))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.RESOURCE_CONFLICT));

		registration.approve(NOW);
		assertThatThrownBy(
				() -> service.reject(MANAGER_EMAIL, registration.getId(), new RejectionRequest("Không phù hợp", 0L)))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.RESOURCE_CONFLICT));
		verify(historyRepository, never()).save(any());
	}

	@Test
	void rejectsBlankReasonAndUnrequestedDevice() {
		assertThatThrownBy(() -> service.reject(MANAGER_EMAIL, registration.getId(), new RejectionRequest("  ", 0L)))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));

		when(registrationRepository.findDetailByIdForUpdate(registration.getId()))
				.thenReturn(Optional.of(registration));
		when(roomRepository.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));
		when(allocationRepository.findRequestedDeviceIds(registration.getId())).thenReturn(List.of());
		assertThatThrownBy(() -> service.approve(MANAGER_EMAIL, registration.getId(),
				new ApprovalRequest(List.of("TB-NOT-REQUESTED"), 0L))).isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
		verify(historyRepository, never()).save(any());
	}

	@Test
	void rejectsPendingRegistrationAndRecordsTrimmedReason() {
		when(registrationRepository.findDetailByIdForUpdate(registration.getId()))
				.thenReturn(Optional.of(registration));

		RegistrationDecisionResponse response = service.reject(MANAGER_EMAIL, registration.getId(),
				new RejectionRequest("  Không đáp ứng điều kiện  ", 0L));

		assertThat(response.status()).isEqualTo(PhieuDangKyTrangThai.TU_CHOI);
		ArgumentCaptor<XuLyPhieu> history = ArgumentCaptor.forClass(XuLyPhieu.class);
		verify(historyRepository).save(history.capture());
		assertThat(history.getValue().getAction()).isEqualTo(HanhDongXuLyPhieu.TU_CHOI);
		assertThat(history.getValue().getReason()).isEqualTo("Không đáp ứng điều kiện");
	}

	private void stubApproval(PhieuDangKy target, List<ThietBi> selectedDevices) {
		List<PhieuDangKyThietBi> allocations = selectedDevices.stream()
				.map(item -> new PhieuDangKyThietBi(target, item, false)).toList();
		when(registrationRepository.findDetailByIdForUpdate(target.getId())).thenReturn(Optional.of(target));
		when(roomRepository.findByIdForUpdate(target.getRoom().getId())).thenReturn(Optional.of(target.getRoom()));
		when(allocationRepository.findRequestedDeviceIds(target.getId()))
				.thenReturn(selectedDevices.stream().map(ThietBi::getId).toList());
		org.mockito.Mockito.lenient().when(allocationRepository.findAllByRegistrationId(target.getId()))
				.thenReturn(allocations);
		if (!selectedDevices.isEmpty()) {
			when(deviceRepository.findAllByIdForUpdateOrderById(selectedDevices.stream().map(ThietBi::getId).toList()))
					.thenReturn(selectedDevices);
		}
		List<TaiNguyen> resources = new java.util.ArrayList<>();
		resources.add(TaiNguyen.forRoom("TN-" + target.getRoom().getId(), target.getRoom()));
		selectedDevices.forEach(item -> resources.add(TaiNguyen.forDevice("TN-" + item.getId(), item)));
		when(resourceRepository.lockForScheduling(any(), any())).thenReturn(resources);
		TietHoc period = new TietHoc(1, "Tiết 1", LocalTime.of(7, 0), LocalTime.of(7, 50));
		when(scheduleRepository.findAllByRegistrationId(target.getId()))
				.thenReturn(List.of(new LichDangKy(target, 2, period)));
		when(schedulingService.checkAvailability(any(), any(), any(), any(), any(Integer.class), any(Integer.class)))
				.thenReturn(new AvailabilityResponse(true, USE_DATE, USE_DATE, 2, "Thứ 2",
						new PeriodResponse(1, "Tiết 1", LocalTime.of(7, 0), LocalTime.of(7, 50)), List.of(USE_DATE),
						List.of()));
	}

	private PhieuDangKy registration(NguoiDung owner, Phong requestedRoom, int attendees) {
		return new PhieuDangKy("PDK-APPROVAL", owner, requestedRoom, LoaiPhieu.NGHIEN_CUU, "Nghiên cứu", attendees,
				USE_DATE, USE_DATE, PhieuDangKyTrangThai.CHO_DUYET);
	}

	private Phong room(int capacity) {
		return new Phong("P-TEST", "Phòng kiểm thử", new NhomPhong("NP", "Nhóm", null), "Tầng 1", capacity,
				PhongTrangThai.SAN_SANG);
	}

	private ThietBi device(String id, boolean controlled, boolean mobile, Phong assignedRoom) {
		LoaiThietBi type = new LoaiThietBi("TYPE-" + id, "Loại " + id, controlled, mobile, null);
		return new ThietBi(id, "Thiết bị " + id, type, "SERIAL-" + id, null, assignedRoom, ThietBiTrangThai.SAN_SANG);
	}

	private NguoiDung user(String id, String email, String roleId) {
		return new NguoiDung(id, id, email, "not-returned", "Khoa CNTT", new VaiTro(roleId, roleId),
				NguoiDungTrangThai.HOAT_DONG);
	}
}
