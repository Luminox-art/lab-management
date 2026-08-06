package com.example.labmanagement.usage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.labmanagement.catalog.domain.NhomPhong;
import com.example.labmanagement.catalog.domain.Phong;
import com.example.labmanagement.catalog.domain.PhongTrangThai;
import com.example.labmanagement.identity.domain.NguoiDung;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.domain.VaiTro;
import com.example.labmanagement.registration.domain.LichDangKy;
import com.example.labmanagement.registration.domain.LoaiPhieu;
import com.example.labmanagement.registration.domain.PhieuDangKy;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import com.example.labmanagement.registration.repository.LichDangKyRepository;
import com.example.labmanagement.registration.repository.PhieuDangKyRepository;
import com.example.labmanagement.scheduling.domain.TietHoc;
import com.example.labmanagement.usage.domain.PhienSuDung;
import com.example.labmanagement.usage.domain.PhienSuDungTrangThai;
import com.example.labmanagement.usage.repository.PhienSuDungRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionGenerationServiceTest {

	@Mock
	private PhieuDangKyRepository registrationRepository;
	@Mock
	private LichDangKyRepository scheduleRepository;
	@Mock
	private PhienSuDungRepository sessionRepository;

	private SessionGenerationService service;
	private PhieuDangKy registration;
	private LichDangKy schedule;

	@BeforeEach
	void setUp() {
		service = new SessionGenerationService(registrationRepository, scheduleRepository, sessionRepository);
		VaiTro role = new VaiTro("GV", "Giảng viên");
		NguoiDung creator = new NguoiDung("GV-USE", "Giảng viên", "gv-use@lab.local", "hash", null, role,
				NguoiDungTrangThai.HOAT_DONG);
		Phong room = new Phong("P-USE", "Phòng dùng", new NhomPhong("N-USE", "Nhóm", null), "A1", 20,
				PhongTrangThai.SAN_SANG);
		registration = new PhieuDangKy("PDK-USE", creator, room, LoaiPhieu.GIANG_DAY, "Thực hành", 10,
				LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 21), PhieuDangKyTrangThai.DA_DUYET);
		schedule = new LichDangKy(registration, 2, new TietHoc(1, "Tiết 1", LocalTime.of(7, 0), LocalTime.of(7, 50)));
		when(registrationRepository.findDetailByIdForUpdate(registration.getId()))
				.thenReturn(Optional.of(registration));
		when(scheduleRepository.findAllByRegistrationId(registration.getId())).thenReturn(List.of(schedule));
	}

	@Test
	void generatesOneSessionForEveryActualScheduleDate() {
		when(sessionRepository.findAllByRegistrationId(registration.getId())).thenReturn(List.of());

		int created = service.generateForRegistration(registration.getId());

		assertThat(created).isEqualTo(3);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<PhienSuDung>> sessions = ArgumentCaptor.forClass(List.class);
		verify(sessionRepository).saveAll(sessions.capture());
		assertThat(sessions.getValue()).extracting(PhienSuDung::getUsageDate).containsExactly(LocalDate.of(2026, 9, 7),
				LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 21));
	}

	@Test
	void doesNotCreateDuplicateForAnExistingScheduleDate() {
		PhienSuDung first = new PhienSuDung(schedule, LocalDate.of(2026, 9, 7), PhienSuDungTrangThai.CHUA_BAT_DAU, null,
				null, null, null);
		PhienSuDung second = new PhienSuDung(schedule, LocalDate.of(2026, 9, 14), PhienSuDungTrangThai.CHUA_BAT_DAU,
				null, null, null, null);
		PhienSuDung third = new PhienSuDung(schedule, LocalDate.of(2026, 9, 21), PhienSuDungTrangThai.CHUA_BAT_DAU,
				null, null, null, null);
		when(sessionRepository.findAllByRegistrationId(registration.getId())).thenReturn(List.of(first, second, third));

		assertThat(service.generateForRegistration(registration.getId())).isZero();

		verify(sessionRepository, never()).saveAll(anyList());
	}
}
