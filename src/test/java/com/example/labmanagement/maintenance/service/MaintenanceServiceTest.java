package com.example.labmanagement.maintenance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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
import com.example.labmanagement.incident.repository.SuCoRepository;
import com.example.labmanagement.maintenance.domain.BaoTri;
import com.example.labmanagement.maintenance.domain.BaoTriTrangThai;
import com.example.labmanagement.maintenance.domain.TienDoBaoTri;
import com.example.labmanagement.maintenance.dto.MaintenanceCreateRequest;
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
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class MaintenanceServiceTest {

	private static final Instant NOW = Instant.parse("2026-08-01T02:00:00Z");
	private static final String MANAGER_EMAIL = "cb-maintenance@lab.local";

	@Mock
	private NguoiDungRepository userRepository;
	@Mock
	private TaiNguyenRepository resourceRepository;
	@Mock
	private SuCoRepository incidentRepository;
	@Mock
	private BaoTriRepository maintenanceRepository;
	@Mock
	private BaoTriSuCoRepository maintenanceIncidentRepository;
	@Mock
	private TienDoBaoTriRepository progressRepository;
	@Mock
	private LichChanRepository blockedScheduleRepository;

	private MaintenanceService service;
	private NguoiDung manager;
	private TaiNguyen resource;
	private final List<TienDoBaoTri> progress = new ArrayList<>();

	@BeforeEach
	void setUp() {
		manager = new NguoiDung("CB-MNT", "Cán bộ bảo trì", MANAGER_EMAIL, "hash", null,
				new VaiTro("CBQL", "Cán bộ quản lý"), NguoiDungTrangThai.HOAT_DONG);
		Phong room = new Phong("P-MNT", "Phòng bảo trì", new NhomPhong("N-MNT", "Nhóm", null), "A1", 20,
				PhongTrangThai.SAN_SANG);
		ThietBi device = new ThietBi("TB-MNT", "Máy đo", new LoaiThietBi("LT-MNT", "Máy đo", false, true, null),
				"SN-MNT", "M1", room, ThietBiTrangThai.SAN_SANG);
		resource = TaiNguyen.forDevice("TN-MNT", device);
		service = new MaintenanceService(userRepository, resourceRepository, incidentRepository, maintenanceRepository,
				maintenanceIncidentRepository, progressRepository, blockedScheduleRepository,
				Clock.fixed(NOW, ZoneOffset.UTC));
		lenient().when(userRepository.findByEmailIgnoreCase(MANAGER_EMAIL)).thenReturn(Optional.of(manager));
		lenient().when(maintenanceIncidentRepository.findByMaintenance_Id(any())).thenReturn(Optional.empty());
		lenient().when(progressRepository.save(any())).thenAnswer(invocation -> {
			TienDoBaoTri item = invocation.getArgument(0);
			progress.add(item);
			return item;
		});
		lenient().when(progressRepository.findAllByMaintenance_IdOrderByOccurredAtAscIdAsc(any()))
				.thenAnswer(invocation -> progress);
	}

	@Test
	void createLinksSameResourceIncidentAndStartsMaintenanceWithInitialTimeline() {
		SuCo source = sourceIncident("SC-MNT", resource);
		when(resourceRepository.findByIdForUpdate("TN-MNT")).thenReturn(Optional.of(resource));
		when(userRepository.findById("CB-MNT")).thenReturn(Optional.of(manager));
		when(incidentRepository.findDetailByIdForUpdate("SC-MNT")).thenReturn(Optional.of(source));
		when(maintenanceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		MaintenanceResponse result = service.create(MANAGER_EMAIL,
				new MaintenanceCreateRequest("TN-MNT", "SC-MNT", "CB-MNT", "Thay bộ nguồn"));

		assertThat(result.status()).isEqualTo(BaoTriTrangThai.CHO_XU_LY);
		assertThat(result.progress()).hasSize(1);
		assertThat(resource.getDevice().getStatus()).isEqualTo(ThietBiTrangThai.BAO_TRI);
	}

	@Test
	void createRejectsSourceIncidentFromAnotherResource() {
		TaiNguyen other = TaiNguyen.forRoom("TN-OTHER", resource.getDevice().getRoom());
		when(resourceRepository.findByIdForUpdate("TN-MNT")).thenReturn(Optional.of(resource));
		when(userRepository.findById("CB-MNT")).thenReturn(Optional.of(manager));
		when(incidentRepository.findDetailByIdForUpdate("SC-OTHER"))
				.thenReturn(Optional.of(sourceIncident("SC-OTHER", other)));

		assertThatThrownBy(() -> service.create(MANAGER_EMAIL,
				new MaintenanceCreateRequest("TN-MNT", "SC-OTHER", "CB-MNT", "Kiểm tra")))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
	}

	@Test
	void everyUpdateAppendsProgressAndCompletionReleasesResourceAndBlock() {
		resource.getDevice().startMaintenance();
		BaoTri maintenance = new BaoTri("BT-MNT", resource, manager, NOW.minusSeconds(3600), null, "Thay nguồn",
				BaoTriTrangThai.CHO_XU_LY, null);
		LichChan blocked = new LichChan(resource, LocalDate.of(2026, 8, 1), LocalDate.of(9999, 12, 31), null, null,
				"Bảo trì", LichChanTrangThai.HIEU_LUC, manager, "BT-MNT");
		when(maintenanceRepository.findDetailByIdForUpdate("BT-MNT")).thenReturn(Optional.of(maintenance));
		when(resourceRepository.findByIdForUpdate("TN-MNT")).thenReturn(Optional.of(resource));
		when(blockedScheduleRepository.findByMaintenanceIdForUpdate("BT-MNT")).thenReturn(Optional.of(blocked));

		service.update(MANAGER_EMAIL, "BT-MNT",
				new MaintenanceUpdateRequest(BaoTriTrangThai.DANG_BAO_TRI, "Đã tháo bộ nguồn", null, null, 0L));
		MaintenanceResponse completed = service.update(MANAGER_EMAIL, "BT-MNT",
				new MaintenanceUpdateRequest(BaoTriTrangThai.HOAN_THANH, "Đã kiểm thử", NOW, "Hoạt động ổn định", 0L));

		assertThat(completed.status()).isEqualTo(BaoTriTrangThai.HOAN_THANH);
		assertThat(completed.result()).isEqualTo("Hoạt động ổn định");
		assertThat(completed.progress()).hasSize(2);
		assertThat(blocked.getStatus()).isEqualTo(LichChanTrangThai.DA_HUY);
		assertThat(resource.getDevice().getStatus()).isEqualTo(ThietBiTrangThai.SAN_SANG);
	}

	@Test
	void cancellingDeviceMaintenanceKeepsTheDeviceUnavailable() {
		resource.getDevice().startMaintenance();
		BaoTri maintenance = new BaoTri("BT-CANCEL", resource, manager, NOW.minusSeconds(3600), null, "Kiểm tra",
				BaoTriTrangThai.CHO_XU_LY, null);
		LichChan blocked = new LichChan(resource, LocalDate.of(2026, 8, 1), LocalDate.of(9999, 12, 31), null, null,
				"Bảo trì", LichChanTrangThai.HIEU_LUC, manager, "BT-CANCEL");
		when(maintenanceRepository.findDetailByIdForUpdate("BT-CANCEL")).thenReturn(Optional.of(maintenance));
		when(resourceRepository.findByIdForUpdate("TN-MNT")).thenReturn(Optional.of(resource));
		when(blockedScheduleRepository.findByMaintenanceIdForUpdate("BT-CANCEL")).thenReturn(Optional.of(blocked));

		service.update(MANAGER_EMAIL, "BT-CANCEL",
				new MaintenanceUpdateRequest(BaoTriTrangThai.DA_HUY, "Không thể tiếp tục", null, null, 0L));

		assertThat(resource.getDevice().getStatus()).isEqualTo(ThietBiTrangThai.HONG);
		assertThat(blocked.getStatus()).isEqualTo(LichChanTrangThai.DA_HUY);
	}

	private SuCo sourceIncident(String id, TaiNguyen incidentResource) {
		return new SuCo(id, incidentResource, null, manager, manager, MucDoSuCo.CAO, "Không khởi động",
				SuCoTrangThai.DANG_XU_LY, NOW.minusSeconds(7200), null, null);
	}
}
