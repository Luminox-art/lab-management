package com.example.labmanagement.scheduling.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.labmanagement.catalog.domain.LoaiTaiNguyen;
import com.example.labmanagement.catalog.domain.NhomPhong;
import com.example.labmanagement.catalog.domain.Phong;
import com.example.labmanagement.catalog.domain.PhongTrangThai;
import com.example.labmanagement.catalog.domain.TaiNguyen;
import com.example.labmanagement.catalog.persistence.PhongRepository;
import com.example.labmanagement.catalog.persistence.TaiNguyenRepository;
import com.example.labmanagement.catalog.persistence.ThietBiRepository;
import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.registration.domain.LichDangKy;
import com.example.labmanagement.registration.domain.LoaiPhieu;
import com.example.labmanagement.registration.domain.PhieuDangKy;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import com.example.labmanagement.registration.persistence.LichDangKyRepository;
import com.example.labmanagement.registration.persistence.PhieuDangKyThietBiRepository;
import com.example.labmanagement.scheduling.domain.LichChan;
import com.example.labmanagement.scheduling.domain.LichChanTrangThai;
import com.example.labmanagement.scheduling.domain.TietHoc;
import com.example.labmanagement.scheduling.persistence.LichChanRepository;
import com.example.labmanagement.scheduling.persistence.TietHocRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** TC-APR-01..06 and TC-REG-01..03 for API-15/16. */
@ExtendWith(MockitoExtension.class)
class SchedulingServiceTest {

	@Mock
	private PhongRepository roomRepository;
	@Mock
	private ThietBiRepository deviceRepository;
	@Mock
	private TaiNguyenRepository resourceRepository;
	@Mock
	private TietHocRepository periodRepository;
	@Mock
	private LichDangKyRepository scheduleRepository;
	@Mock
	private PhieuDangKyThietBiRepository allocationRepository;
	@Mock
	private LichChanRepository blockedScheduleRepository;

	private SchedulingService service;
	private Phong room;
	private TietHoc period;

	@BeforeEach
	void setUp() {
		service = new SchedulingService(roomRepository, deviceRepository, resourceRepository, periodRepository,
				scheduleRepository, allocationRepository, blockedScheduleRepository);
		room = new Phong("P-TEST", "Phòng test", new NhomPhong("NP", "Nhóm", null), "Tầng 1", 20,
				PhongTrangThai.SAN_SANG);
		period = new TietHoc(1, "Tiết 1", LocalTime.of(7, 0), LocalTime.of(7, 50));
	}

	@Test
	void approvedRoomScheduleCreatesConflictOnlyOnActualIntersectingDates() {
		PhieuDangKy registration = registration(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
				PhieuDangKyTrangThai.DA_DUYET);
		LichDangKy schedule = new LichDangKy(registration, 2, period);
		stubRoomAndPeriod();
		when(scheduleRepository.findRoomCandidates(any(), any(), any(), any())).thenReturn(List.of(schedule));

		AvailabilityResponse response = service.checkAvailability("P-TEST", List.of(), LocalDate.of(2026, 9, 1),
				LocalDate.of(2026, 9, 30), 2, 1);

		assertThat(response.available()).isFalse();
		assertThat(response.conflicts()).hasSize(4).allSatisfy(conflict -> {
			assertThat(conflict.type()).isEqualTo(AvailabilityConflictType.ROOM_REGISTRATION);
			assertThat(conflict.resourceType()).isEqualTo(LoaiTaiNguyen.PHONG);
		});
		assertThat(response.conflicts()).extracting(AvailabilityConflictResponse::date).containsExactly(
				LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 21),
				LocalDate.of(2026, 9, 28));
	}

	@Test
	void overlappingRangesWithoutRequestedWeekdayDoNotConflict() {
		PhieuDangKy registration = registration(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1),
				PhieuDangKyTrangThai.DA_DUYET);
		LichDangKy schedule = new LichDangKy(registration, 2, period);
		stubRoomAndPeriod();
		when(scheduleRepository.findRoomCandidates(any(), any(), any(), any())).thenReturn(List.of(schedule));

		AvailabilityResponse response = service.checkAvailability("P-TEST", List.of(), LocalDate.of(2026, 8, 31),
				LocalDate.of(2026, 9, 1), 2, 1);

		assertThat(response.available()).isTrue();
		assertThat(response.conflicts()).isEmpty();
	}

	@Test
	void blockWithNullDayAndPeriodBlocksEveryRequestedSlot() {
		TaiNguyen resource = TaiNguyen.forRoom("TN-P-TEST", room);
		LichChan blocked = new LichChan(resource, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), null, null,
				"Bảo trì tổng thể", LichChanTrangThai.HIEU_LUC, null);
		stubRoomAndPeriod();
		when(resourceRepository.findByRoom_Id("P-TEST")).thenReturn(Optional.of(resource));
		when(blockedScheduleRepository.findCandidates(any(), any(), any(), any())).thenReturn(List.of(blocked));

		AvailabilityResponse response = service.checkAvailability("P-TEST", List.of(), LocalDate.of(2026, 9, 1),
				LocalDate.of(2026, 9, 15), 4, 1);

		assertThat(response.available()).isFalse();
		assertThat(response.conflicts()).hasSize(2).allSatisfy(conflict -> {
			assertThat(conflict.type()).isEqualTo(AvailabilityConflictType.BLOCKED_SCHEDULE);
			assertThat(conflict.message()).startsWith("Lịch chặn cả ngày");
		});
	}

	@Test
	void validatesResourceSelectionDateRangeAndSystemDay() {
		assertThatThrownBy(() -> service.checkAvailability(null, List.of(), LocalDate.of(2026, 9, 1),
				LocalDate.of(2026, 9, 2), 2, 1)).isInstanceOf(ApiException.class).hasMessageContaining("ít nhất một");
		assertThatThrownBy(() -> service.checkAvailability("P-TEST", List.of(), LocalDate.of(2026, 9, 2),
				LocalDate.of(2026, 9, 1), 2, 1)).isInstanceOf(ApiException.class).hasMessageContaining("Ngày bắt đầu");
		assertThatThrownBy(() -> service.checkAvailability("P-TEST", List.of(), LocalDate.of(2026, 9, 1),
				LocalDate.of(2026, 9, 2), 9, 1)).isInstanceOf(ApiException.class).hasMessageContaining("2 đến 8");
	}

	private void stubRoomAndPeriod() {
		when(roomRepository.findById("P-TEST")).thenReturn(Optional.of(room));
		when(periodRepository.findById(1)).thenReturn(Optional.of(period));
	}

	private PhieuDangKy registration(LocalDate from, LocalDate to, PhieuDangKyTrangThai status) {
		return new PhieuDangKy("PDK-TEST", null, room, LoaiPhieu.HOC_TAP, "Không trả qua API", 10, from, to, status);
	}
}
