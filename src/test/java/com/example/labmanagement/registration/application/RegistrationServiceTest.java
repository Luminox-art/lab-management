package com.example.labmanagement.registration.application;

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
import com.example.labmanagement.catalog.domain.ThietBi;
import com.example.labmanagement.catalog.domain.ThietBiTrangThai;
import com.example.labmanagement.catalog.persistence.PhongRepository;
import com.example.labmanagement.catalog.persistence.ThietBiRepository;
import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.common.error.ErrorCode;
import com.example.labmanagement.identity.domain.NguoiDung;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.domain.VaiTro;
import com.example.labmanagement.identity.persistence.NguoiDungRepository;
import com.example.labmanagement.registration.domain.HanhDongXuLyPhieu;
import com.example.labmanagement.registration.domain.LichDangKy;
import com.example.labmanagement.registration.domain.LoaiPhieu;
import com.example.labmanagement.registration.domain.PhieuDangKy;
import com.example.labmanagement.registration.domain.PhieuDangKyThietBi;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import com.example.labmanagement.registration.domain.PhieuGiangDay;
import com.example.labmanagement.registration.domain.XuLyPhieu;
import com.example.labmanagement.registration.persistence.LichDangKyRepository;
import com.example.labmanagement.registration.persistence.PhieuDangKyRepository;
import com.example.labmanagement.registration.persistence.PhieuDangKyThietBiRepository;
import com.example.labmanagement.registration.persistence.PhieuGiangDayRepository;
import com.example.labmanagement.registration.persistence.PhieuHuongDanRepository;
import com.example.labmanagement.registration.persistence.XuLyPhieuRepository;
import com.example.labmanagement.scheduling.domain.TietHoc;
import com.example.labmanagement.scheduling.persistence.TietHocRepository;
import com.example.labmanagement.usage.persistence.PhienSuDungRepository;
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

/** TC-REG-04..25 service rules for API-17..21. */
@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

	private static final Instant NOW = Instant.parse("2026-08-01T00:00:00Z");
	private static final LocalDate START_DATE = LocalDate.of(2026, 9, 1);
	private static final LocalDate END_DATE = LocalDate.of(2026, 9, 30);

	@Mock
	private NguoiDungRepository userRepository;
	@Mock
	private PhongRepository roomRepository;
	@Mock
	private ThietBiRepository deviceRepository;
	@Mock
	private TietHocRepository periodRepository;
	@Mock
	private PhieuDangKyRepository registrationRepository;
	@Mock
	private LichDangKyRepository scheduleRepository;
	@Mock
	private PhieuDangKyThietBiRepository allocationRepository;
	@Mock
	private PhieuGiangDayRepository teachingRepository;
	@Mock
	private PhieuHuongDanRepository supervisionRepository;
	@Mock
	private XuLyPhieuRepository historyRepository;
	@Mock
	private PhienSuDungRepository sessionRepository;
	@Mock
	private EntityManager entityManager;

	private RegistrationService service;
	private VaiTro instructorRole;
	private VaiTro studentRole;
	private VaiTro managerRole;
	private NguoiDung instructor;
	private NguoiDung student;
	private Phong room;
	private TietHoc firstPeriod;
	private TietHoc secondPeriod;

	@BeforeEach
	void setUp() {
		service = new RegistrationService(userRepository, roomRepository, deviceRepository, periodRepository,
				registrationRepository, scheduleRepository, allocationRepository, teachingRepository,
				supervisionRepository, historyRepository, sessionRepository, entityManager,
				Clock.fixed(NOW, ZoneOffset.UTC));
		instructorRole = new VaiTro("GV", "Giảng viên");
		studentRole = new VaiTro("SV", "Sinh viên");
		managerRole = new VaiTro("CBQL", "Cán bộ quản lý");
		instructor = user("GV-TEST", "gv-test@lab.local", instructorRole, NguoiDungTrangThai.HOAT_DONG);
		student = user("SV-TEST", "sv-test@lab.local", studentRole, NguoiDungTrangThai.HOAT_DONG);
		room = new Phong("P-TEST", "Phòng kiểm thử", new NhomPhong("NP", "Nhóm", null), "Tầng 1", 30,
				PhongTrangThai.SAN_SANG);
		firstPeriod = new TietHoc(1, "Tiết 1", LocalTime.of(7, 0), LocalTime.of(7, 50));
		secondPeriod = new TietHoc(2, "Tiết 2", LocalTime.of(8, 0), LocalTime.of(8, 50));
	}

	@Test
	void lecturerCreatesTeachingAggregateWithSchedulesAndRequestedDevice() {
		stubActor(instructor);
		when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
		when(periodRepository.findAllById(List.of(1, 2))).thenReturn(List.of(firstPeriod, secondPeriod));
		ThietBi device = device("TB-TEST", false, false, room, ThietBiTrangThai.SAN_SANG);
		when(deviceRepository.findAllById(List.of(device.getId()))).thenReturn(List.of(device));
		when(registrationRepository.save(any(PhieuDangKy.class))).thenAnswer(invocation -> invocation.getArgument(0));

		RegistrationResponse response = service.create(instructor.getEmail(),
				teachingRequest(20,
						List.of(new RegistrationScheduleRequest(2, 1), new RegistrationScheduleRequest(4, 2)),
						List.of(device.getId()), null));

		ArgumentCaptor<PhieuDangKy> registrationCaptor = ArgumentCaptor.forClass(PhieuDangKy.class);
		verify(registrationRepository).save(registrationCaptor.capture());
		PhieuDangKy saved = registrationCaptor.getValue();
		assertThat(saved.getCreator()).isSameAs(instructor);
		assertThat(saved.getType()).isEqualTo(LoaiPhieu.GIANG_DAY);
		assertThat(saved.getStatus()).isEqualTo(PhieuDangKyTrangThai.CHO_DUYET);
		assertThat(saved.getAttendeeCount()).isEqualTo(20);
		assertThat(response.id()).isEqualTo(saved.getId());
		assertThat(response.status()).isEqualTo(PhieuDangKyTrangThai.CHO_DUYET);

		verify(entityManager).persist(org.mockito.ArgumentMatchers.argThat(entity -> {
			if (!(entity instanceof PhieuGiangDay teaching)) {
				return false;
			}
			assertThat(teaching.getRegistration()).isSameAs(saved);
			assertThat(teaching.getCourseId()).isEqualTo("INT1234");
			assertThat(teaching.getClassGroupName()).isEqualTo("01");
			return true;
		}));
		verify(scheduleRepository).saveAll(org.mockito.ArgumentMatchers.argThat(schedules -> {
			assertThat(schedules).extracting(LichDangKy::getDayOfWeek).containsExactly((byte) 2, (byte) 4);
			assertThat(schedules).extracting(schedule -> schedule.getPeriod().getId()).containsExactly(1, 2);
			return true;
		}));
		verify(entityManager).persist(org.mockito.ArgumentMatchers.argThat(entity -> {
			if (!(entity instanceof PhieuDangKyThietBi allocation)) {
				return false;
			}
			assertThat(allocation.getRegistration()).isSameAs(saved);
			assertThat(allocation.getDevice()).isSameAs(device);
			assertThat(allocation.isAllocated()).isFalse();
			return true;
		}));
	}

	@Test
	void studentCannotCreateTeachingRegistration() {
		stubActor(student);

		assertThatThrownBy(() -> service.create(student.getEmail(),
				teachingRequest(20, List.of(new RegistrationScheduleRequest(2, 1)), List.of(), null)))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.ACCESS_DENIED));

		verifyNoAggregateWrites();
		verify(roomRepository, never()).findById(any());
	}

	@Test
	void capacityViolationIsRejectedBeforeWritingAggregate() {
		stubActor(instructor);
		when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));

		assertThatThrownBy(() -> service.create(instructor.getEmail(),
				teachingRequest(31, List.of(new RegistrationScheduleRequest(2, 1)), List.of(), null)))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION));

		verifyNoAggregateWrites();
		verify(periodRepository, never()).findAllById(any());
	}

	@Test
	void excessivePurposeAndDateRangeAreRejectedBeforeScheduleExpansion() {
		stubActor(instructor);
		RegistrationFormRequest excessivePurpose = new RegistrationFormRequest(LoaiPhieu.GIANG_DAY, "x".repeat(2001),
				room.getId(), 20, START_DATE, END_DATE, List.of(new RegistrationScheduleRequest(2, 1)), List.of(),
				"INT1234", "01", null, null);

		assertThatThrownBy(() -> service.create(instructor.getEmail(), excessivePurpose)).isInstanceOfSatisfying(
				ApiException.class, exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));

		when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
		RegistrationFormRequest excessiveRange = new RegistrationFormRequest(LoaiPhieu.GIANG_DAY, "Thực hành mạng",
				room.getId(), 20, START_DATE, START_DATE.plusDays(3660), List.of(new RegistrationScheduleRequest(2, 1)),
				List.of(), "INT1234", "01", null, null);
		assertThatThrownBy(() -> service.create(instructor.getEmail(), excessiveRange)).isInstanceOfSatisfying(
				ApiException.class, exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));

		verifyNoAggregateWrites();
		verify(periodRepository, never()).findAllById(any());
	}

	@Test
	void duplicateScheduleIsRejectedBeforeWritingAggregate() {
		stubActor(instructor);
		when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));

		assertThatThrownBy(
				() -> service
						.create(instructor.getEmail(),
								teachingRequest(20,
										List.of(new RegistrationScheduleRequest(2, 1),
												new RegistrationScheduleRequest(2, 1)),
										List.of(), null)))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));

		verifyNoAggregateWrites();
		verify(periodRepository, never()).findAllById(any());
	}

	@Test
	void fixedDeviceFromAnotherRoomIsRejectedBeforeWritingAggregate() {
		stubActor(instructor);
		when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
		when(periodRepository.findAllById(List.of(1))).thenReturn(List.of(firstPeriod));
		Phong otherRoom = new Phong("P-OTHER", "Phòng khác", room.getGroup(), "Tầng 2", 30, PhongTrangThai.SAN_SANG);
		ThietBi fixedDevice = device("TB-FIXED", false, false, otherRoom, ThietBiTrangThai.SAN_SANG);
		when(deviceRepository.findAllById(List.of(fixedDevice.getId()))).thenReturn(List.of(fixedDevice));

		assertThatThrownBy(() -> service.create(instructor.getEmail(),
				studyRequest(instructor, 20, List.of(new RegistrationScheduleRequest(2, 1)),
						List.of(fixedDevice.getId()), null, null)))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION));

		verifyNoAggregateWrites();
	}

	@Test
	void controlledDeviceRequiresActiveInstructorBeforeWritingAggregate() {
		stubActor(student);
		when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
		when(periodRepository.findAllById(List.of(1))).thenReturn(List.of(firstPeriod));
		ThietBi controlled = device("TB-ROBOT", true, true, null, ThietBiTrangThai.SAN_SANG);
		when(deviceRepository.findAllById(List.of(controlled.getId()))).thenReturn(List.of(controlled));
		NguoiDung blockedSupervisor = user("GV-BLOCKED", "gv-blocked@lab.local", instructorRole,
				NguoiDungTrangThai.BI_KHOA);
		when(userRepository.findById(blockedSupervisor.getId())).thenReturn(Optional.of(blockedSupervisor));

		assertThatThrownBy(() -> service.create(student.getEmail(),
				studyRequest(student, 10, List.of(new RegistrationScheduleRequest(2, 1)), List.of(controlled.getId()),
						null, null)))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION));
		assertThatThrownBy(() -> service.create(student.getEmail(),
				studyRequest(student, 10, List.of(new RegistrationScheduleRequest(2, 1)), List.of(controlled.getId()),
						blockedSupervisor.getId(), null)))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION));

		verifyNoAggregateWrites();
	}

	@Test
	void ownerSupervisorAndManagerCanViewWhileUnrelatedStudentCannot() {
		NguoiDung supervisor = user("GV-SUP", "gv-sup@lab.local", instructorRole, NguoiDungTrangThai.HOAT_DONG);
		NguoiDung manager = user("CB-TEST", "cb-test@lab.local", managerRole, NguoiDungTrangThai.HOAT_DONG);
		NguoiDung outsider = user("SV-OTHER", "sv-other@lab.local", studentRole, NguoiDungTrangThai.HOAT_DONG);
		stubActor(student);
		stubActor(supervisor);
		stubActor(manager);
		stubActor(outsider);
		PhieuDangKy registration = registration("PDK-VIEW", student, PhieuDangKyTrangThai.CHO_DUYET, START_DATE,
				END_DATE);
		when(registrationRepository.findDetailById(registration.getId())).thenReturn(Optional.of(registration));
		when(supervisionRepository.existsByRegistration_IdAndInstructor_Id(registration.getId(), supervisor.getId()))
				.thenReturn(true);
		stubEmptyDetail(registration.getId());

		assertThat(service.get(student.getEmail(), registration.getId()).id()).isEqualTo(registration.getId());
		assertThat(service.get(supervisor.getEmail(), registration.getId()).id()).isEqualTo(registration.getId());
		assertThat(service.get(manager.getEmail(), registration.getId()).id()).isEqualTo(registration.getId());
		assertThatThrownBy(() -> service.get(outsider.getEmail(), registration.getId())).isInstanceOfSatisfying(
				ApiException.class, exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
	}

	@Test
	void updateRejectsStaleVersionAndNonPendingRegistrationBeforeReplacingChildren() {
		stubActor(student);
		PhieuDangKy stale = registration("PDK-STALE", student, PhieuDangKyTrangThai.CHO_DUYET, START_DATE, END_DATE);
		PhieuDangKy approved = registration("PDK-APPROVED", student, PhieuDangKyTrangThai.DA_DUYET, START_DATE,
				END_DATE);
		when(registrationRepository.findDetailById(stale.getId())).thenReturn(Optional.of(stale));
		when(registrationRepository.findDetailById(approved.getId())).thenReturn(Optional.of(approved));
		RegistrationFormRequest staleRequest = studyRequest(student, 10, List.of(new RegistrationScheduleRequest(2, 1)),
				List.of(), null, 1L);
		RegistrationFormRequest currentRequest = studyRequest(student, 10,
				List.of(new RegistrationScheduleRequest(2, 1)), List.of(), null, 0L);

		assertThatThrownBy(() -> service.update(student.getEmail(), stale.getId(), staleRequest))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.RESOURCE_CONFLICT));
		assertThatThrownBy(() -> service.update(student.getEmail(), approved.getId(), currentRequest))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.RESOURCE_CONFLICT));

		verify(scheduleRepository, never()).findAllByRegistrationId(any());
		verify(allocationRepository, never()).findAllByRegistrationId(any());
	}

	@Test
	void ownerCancelsFutureApprovedRegistrationAndCreatesHistory() {
		stubActor(student);
		LocalDate usageDate = LocalDate.of(2026, 9, 7);
		PhieuDangKy registration = registration("PDK-CANCEL", student, PhieuDangKyTrangThai.DA_DUYET, usageDate,
				usageDate);
		LichDangKy schedule = new LichDangKy(registration, 2, firstPeriod);
		when(registrationRepository.findDetailById(registration.getId())).thenReturn(Optional.of(registration));
		when(scheduleRepository.findAllByRegistrationId(registration.getId())).thenReturn(List.of(schedule));
		when(sessionRepository.existsStartedByRegistrationId(registration.getId())).thenReturn(false);

		RegistrationResponse response = service.cancel(student.getEmail(), registration.getId(),
				new RegistrationCancelRequest("  Không còn nhu cầu  ", 0L));

		assertThat(registration.getStatus()).isEqualTo(PhieuDangKyTrangThai.DA_HUY);
		assertThat(response.status()).isEqualTo(PhieuDangKyTrangThai.DA_HUY);
		ArgumentCaptor<XuLyPhieu> historyCaptor = ArgumentCaptor.forClass(XuLyPhieu.class);
		verify(historyRepository).save(historyCaptor.capture());
		assertThat(historyCaptor.getValue().getAction()).isEqualTo(HanhDongXuLyPhieu.HUY);
		assertThat(historyCaptor.getValue().getHandler()).isSameAs(student);
		assertThat(historyCaptor.getValue().getReason()).isEqualTo("Không còn nhu cầu");
		assertThat(historyCaptor.getValue().getOccurredAt()).isEqualTo(NOW);
		verify(registrationRepository).flush();
	}

	@Test
	void cancelRejectsStartedSessionAndPastFirstOccurrence() {
		stubActor(student);
		LocalDate futureDate = LocalDate.of(2026, 9, 7);
		PhieuDangKy started = registration("PDK-STARTED", student, PhieuDangKyTrangThai.DA_DUYET, futureDate,
				futureDate);
		LichDangKy futureSchedule = new LichDangKy(started, 2, firstPeriod);
		LocalDate pastDate = LocalDate.of(2026, 7, 6);
		PhieuDangKy past = registration("PDK-PAST", student, PhieuDangKyTrangThai.DA_DUYET, pastDate, pastDate);
		LichDangKy pastSchedule = new LichDangKy(past, 2, firstPeriod);
		when(registrationRepository.findDetailById(started.getId())).thenReturn(Optional.of(started));
		when(registrationRepository.findDetailById(past.getId())).thenReturn(Optional.of(past));
		when(scheduleRepository.findAllByRegistrationId(started.getId())).thenReturn(List.of(futureSchedule));
		when(scheduleRepository.findAllByRegistrationId(past.getId())).thenReturn(List.of(pastSchedule));
		when(sessionRepository.existsStartedByRegistrationId(started.getId())).thenReturn(true);
		when(sessionRepository.existsStartedByRegistrationId(past.getId())).thenReturn(false);

		RegistrationCancelRequest request = new RegistrationCancelRequest("Không còn nhu cầu", 0L);
		assertThatThrownBy(() -> service.cancel(student.getEmail(), started.getId(), request)).isInstanceOfSatisfying(
				ApiException.class,
				exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.RESOURCE_CONFLICT));
		assertThatThrownBy(() -> service.cancel(student.getEmail(), past.getId(), request)).isInstanceOfSatisfying(
				ApiException.class,
				exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.RESOURCE_CONFLICT));

		verify(historyRepository, never()).save(any());
		assertThat(started.getStatus()).isEqualTo(PhieuDangKyTrangThai.DA_DUYET);
		assertThat(past.getStatus()).isEqualTo(PhieuDangKyTrangThai.DA_DUYET);
	}

	private void stubActor(NguoiDung actor) {
		when(userRepository.findByEmailIgnoreCase(actor.getEmail())).thenReturn(Optional.of(actor));
	}

	private void stubEmptyDetail(String registrationId) {
		when(teachingRepository.findByRegistrationId(registrationId)).thenReturn(Optional.empty());
		when(supervisionRepository.findByRegistrationId(registrationId)).thenReturn(Optional.empty());
		when(scheduleRepository.findAllByRegistrationId(registrationId)).thenReturn(List.of());
		when(allocationRepository.findAllByRegistrationId(registrationId)).thenReturn(List.of());
		when(historyRepository.findAllByRegistrationId(registrationId)).thenReturn(List.of());
	}

	private void verifyNoAggregateWrites() {
		verify(registrationRepository, never()).save(any());
		verify(entityManager, never()).persist(any());
		verify(scheduleRepository, never()).saveAll(any());
		verify(allocationRepository, never()).saveAll(any());
	}

	private RegistrationFormRequest teachingRequest(int participants, List<RegistrationScheduleRequest> schedules,
			List<String> deviceIds, Long version) {
		return new RegistrationFormRequest(LoaiPhieu.GIANG_DAY, "Thực hành mạng", room.getId(), participants,
				START_DATE, END_DATE, schedules, deviceIds, "INT1234", "01", null, version);
	}

	private RegistrationFormRequest studyRequest(NguoiDung actor, int participants,
			List<RegistrationScheduleRequest> schedules, List<String> deviceIds, String supervisorId, Long version) {
		return new RegistrationFormRequest(LoaiPhieu.NGHIEN_CUU, "Nghiên cứu", room.getId(), participants, START_DATE,
				END_DATE, schedules, deviceIds, null, null, "SV".equals(actor.getRole().getId()) ? supervisorId : null,
				version);
	}

	private PhieuDangKy registration(String id, NguoiDung creator, PhieuDangKyTrangThai status, LocalDate startDate,
			LocalDate endDate) {
		return new PhieuDangKy(id, creator, room, LoaiPhieu.NGHIEN_CUU, "Nghiên cứu", 10, startDate, endDate, status);
	}

	private NguoiDung user(String id, String email, VaiTro role, NguoiDungTrangThai status) {
		return new NguoiDung(id, id, email, "not-returned", "Khoa CNTT", role, status);
	}

	private ThietBi device(String id, boolean instructorRequired, boolean mobile, Phong assignedRoom,
			ThietBiTrangThai status) {
		LoaiThietBi type = new LoaiThietBi("TYPE-" + id, "Loại " + id, instructorRequired, mobile, null);
		return new ThietBi(id, "Thiết bị " + id, type, "SERIAL-" + id, null, assignedRoom, status);
	}
}
