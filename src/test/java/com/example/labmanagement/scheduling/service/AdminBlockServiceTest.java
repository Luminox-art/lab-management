package com.example.labmanagement.scheduling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.labmanagement.catalog.domain.NhomPhong;
import com.example.labmanagement.catalog.domain.Phong;
import com.example.labmanagement.catalog.domain.PhongTrangThai;
import com.example.labmanagement.catalog.domain.TaiNguyen;
import com.example.labmanagement.catalog.repository.PhongRepository;
import com.example.labmanagement.catalog.repository.TaiNguyenRepository;
import com.example.labmanagement.catalog.repository.ThietBiRepository;
import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.identity.domain.NguoiDung;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.domain.VaiTro;
import com.example.labmanagement.identity.repository.NguoiDungRepository;
import com.example.labmanagement.registration.domain.LichDangKy;
import com.example.labmanagement.registration.domain.LoaiPhieu;
import com.example.labmanagement.registration.domain.PhieuDangKy;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import com.example.labmanagement.registration.repository.LichDangKyRepository;
import com.example.labmanagement.registration.repository.PhieuDangKyThietBiRepository;
import com.example.labmanagement.scheduling.domain.LichChan;
import com.example.labmanagement.scheduling.domain.LichChanTrangThai;
import com.example.labmanagement.scheduling.domain.TietHoc;
import com.example.labmanagement.scheduling.dto.AdminBlockCreationResponse;
import com.example.labmanagement.scheduling.dto.AdminBlockRequest;
import com.example.labmanagement.scheduling.dto.AdminBlockResponse;
import com.example.labmanagement.scheduling.repository.LichChanRepository;
import com.example.labmanagement.scheduling.repository.TietHocRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class AdminBlockServiceTest {

	private static final LocalDate MONDAY = LocalDate.of(2035, 1, 1);

	@Mock
	private NguoiDungRepository userRepository;

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

	@Mock
	private EntityManager entityManager;

	private AdminBlockService service;
	private NguoiDung manager;
	private Phong room;
	private TaiNguyen resource;
	private TietHoc firstPeriod;

	@BeforeEach
	void setUp() {
		manager = new NguoiDung("CB001", "Cán bộ quản lý", "cb001@lab.local", "hash", null,
				new VaiTro("CBQL", "Cán bộ quản lý"), NguoiDungTrangThai.HOAT_DONG);
		room = new Phong("P0601", "Phòng 6.1", new NhomPhong("NP01", "Phòng máy", null), "Tầng 6", 40,
				PhongTrangThai.SAN_SANG);
		resource = TaiNguyen.forRoom("TN-P0601", room);
		firstPeriod = new TietHoc(1, "Tiết 1", LocalTime.of(7, 0), LocalTime.of(7, 50));
		service = new AdminBlockService(userRepository, roomRepository, deviceRepository, resourceRepository,
				periodRepository, scheduleRepository, allocationRepository, blockedScheduleRepository, entityManager);
		when(userRepository.findByEmailIgnoreCase("cb001@lab.local")).thenReturn(Optional.of(manager));
	}

	@Test
	void createsOneEffectiveBlockForEverySelectedPeriodUnderTheSharedResourceLock() {
		TietHoc secondPeriod = new TietHoc(2, "Tiết 2", LocalTime.of(7, 50), LocalTime.of(8, 40));
		prepareRoomLock();
		when(periodRepository.findAllById(List.of(1, 2))).thenReturn(List.of(secondPeriod, firstPeriod));
		when(blockedScheduleRepository.findCandidates(any(), any(), any(), any())).thenReturn(List.of());
		when(scheduleRepository.findRoomCandidates(any(), any(), any(), any())).thenReturn(List.of());
		when(blockedScheduleRepository.save(any(LichChan.class))).thenAnswer(invocation -> invocation.getArgument(0));

		AdminBlockCreationResponse result = service.create("cb001@lab.local", new AdminBlockRequest(" P0601 ", null,
				MONDAY, MONDAY.plusWeeks(2), 2, List.of(2, 1), "  Bảo trì định kỳ  "));

		assertThat(result.blocks()).hasSize(2).extracting(AdminBlockResponse::periodId).containsExactly(1, 2);
		assertThat(result.blocks()).allSatisfy(block -> {
			assertThat(block.status()).isEqualTo(LichChanTrangThai.HIEU_LUC);
			assertThat(block.reason()).isEqualTo("Bảo trì định kỳ");
		});
		verify(resourceRepository).lockForScheduling("P0601", List.of("__NO_DEVICE__"));
		verify(blockedScheduleRepository).flush();
	}

	@Test
	void emptyPeriodSelectionCreatesOneAllDayBlock() {
		prepareRoomLock();
		when(blockedScheduleRepository.findCandidates(any(), any(), any(), any())).thenReturn(List.of());
		when(scheduleRepository.findRoomCandidates(any(), any(), any(), any())).thenReturn(List.of());
		when(blockedScheduleRepository.save(any(LichChan.class))).thenAnswer(invocation -> invocation.getArgument(0));

		AdminBlockCreationResponse result = service.create("cb001@lab.local",
				new AdminBlockRequest("P0601", null, MONDAY, MONDAY, null, List.of(), "Sự kiện toàn ngày"));

		assertThat(result.blocks()).singleElement().satisfies(block -> {
			assertThat(block.periodId()).isNull();
			assertThat(block.periodName()).isEqualTo("Cả ngày");
			assertThat(block.dayLabel()).isEqualTo("Mỗi ngày");
		});
	}

	@Test
	void rejectsAmbiguousResourceAndDateRangeWithoutWriting() {
		assertThatThrownBy(() -> service.create("cb001@lab.local",
				new AdminBlockRequest("P0601", "TB0001", MONDAY.plusDays(1), MONDAY, 2, List.of(1), "Sai")))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
		verify(blockedScheduleRepository, never()).save(any());
	}

	@Test
	void rejectsBlockThatOverlapsAnApprovedRegistration() {
		prepareRoomLock();
		when(periodRepository.findAllById(List.of(1))).thenReturn(List.of(firstPeriod));
		when(blockedScheduleRepository.findCandidates(any(), any(), any(), any())).thenReturn(List.of());
		PhieuDangKy registration = new PhieuDangKy("PDK001", manager, room, LoaiPhieu.NGHIEN_CUU, "Nghiên cứu", 5,
				MONDAY, MONDAY, PhieuDangKyTrangThai.DA_DUYET);
		when(scheduleRepository.findRoomCandidates(any(), any(), any(), any()))
				.thenReturn(List.of(new LichDangKy(registration, 2, firstPeriod)));

		assertThatThrownBy(() -> service.create("cb001@lab.local",
				new AdminBlockRequest("P0601", null, MONDAY, MONDAY, 2, List.of(1), "Trùng lịch")))
				.isInstanceOfSatisfying(ApiException.class, exception -> {
					assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
					assertThat(exception.getMessage()).contains("phiếu được duyệt");
				});
		verify(blockedScheduleRepository, never()).save(any());
	}

	@Test
	void cancellationUsesTheSameLockAndOnlyChangesStatus() {
		LichChan blocked = new LichChan(resource, MONDAY, MONDAY, (byte) 2, firstPeriod, "Bảo trì",
				LichChanTrangThai.HIEU_LUC, manager);
		when(blockedScheduleRepository.findDetailById(10L)).thenReturn(Optional.of(blocked));
		prepareRoomLock();
		when(blockedScheduleRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(blocked));

		service.cancel("cb001@lab.local", 10L);

		assertThat(blocked.getStatus()).isEqualTo(LichChanTrangThai.DA_HUY);
		verify(resourceRepository).lockForScheduling("P0601", List.of("__NO_DEVICE__"));
		verify(entityManager).refresh(blocked);
		verify(blockedScheduleRepository).flush();
	}

	private void prepareRoomLock() {
		when(roomRepository.findByIdForUpdate("P0601")).thenReturn(Optional.of(room));
		when(resourceRepository.lockForScheduling("P0601", List.of("__NO_DEVICE__"))).thenReturn(List.of(resource));
	}
}
