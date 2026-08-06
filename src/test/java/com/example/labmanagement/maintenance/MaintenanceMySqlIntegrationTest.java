package com.example.labmanagement.maintenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.maintenance.domain.BaoTriTrangThai;
import com.example.labmanagement.maintenance.dto.MaintenanceCreateRequest;
import com.example.labmanagement.maintenance.dto.MaintenanceResponse;
import com.example.labmanagement.maintenance.dto.MaintenanceUpdateRequest;
import com.example.labmanagement.maintenance.service.MaintenanceService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(MaintenanceMySqlIntegrationTest.FixedClockConfiguration.class)
@EnabledIfEnvironmentVariable(named = "LAB_TEST_DB_PASSWORD", matches = ".+")
class MaintenanceMySqlIntegrationTest {

	private static final String MANAGER_EMAIL = "cb001@lab.local";
	private static final Instant NOW = Instant.parse("2026-08-01T02:00:00Z");

	@Autowired
	private MaintenanceService maintenanceService;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	@AfterEach
	void cleanStageTenRows() {
		jdbcTemplate.update("""
				DELETE block FROM LichChan block
				JOIN BaoTri maintenance ON maintenance.MaBaoTri = block.MaBaoTri
				JOIN BaoTriSuCo source ON source.MaBaoTri = maintenance.MaBaoTri
				WHERE source.MaSuCo LIKE 'S10-%'
				""");
		jdbcTemplate.update("""
				DELETE maintenance FROM BaoTri maintenance
				JOIN BaoTriSuCo source ON source.MaBaoTri = maintenance.MaBaoTri
				WHERE source.MaSuCo LIKE 'S10-%'
				""");
		jdbcTemplate.update("DELETE FROM SuCo WHERE MaSuCo LIKE 'S10-%'");
		jdbcTemplate.update("UPDATE ThietBi SET TrangThai = 'SAN_SANG' WHERE MaThietBi IN ('TB0001','TB0002')");
	}

	@Test
	void createsProgressesAndCompletesWithOneConsistentResourceBlockLifecycle() {
		insertIncident("S10-SC-HAPPY", "TN-TB0001");

		MaintenanceResponse created = maintenanceService.create(MANAGER_EMAIL,
				new MaintenanceCreateRequest("TN-TB0001", "S10-SC-HAPPY", "CB001", "Thay bộ nguồn"));
		assertThat(created.status()).isEqualTo(BaoTriTrangThai.CHO_XU_LY);
		assertThat(value("SELECT TrangThai FROM ThietBi WHERE MaThietBi = 'TB0001'", String.class))
				.isEqualTo("BAO_TRI");
		assertThat(value("SELECT COUNT(*) FROM LichChan WHERE MaBaoTri = ? AND TrangThai = 'HIEU_LUC'", Integer.class,
				created.id())).isEqualTo(1);

		MaintenanceResponse active = maintenanceService.update(MANAGER_EMAIL, created.id(),
				new MaintenanceUpdateRequest(BaoTriTrangThai.DANG_BAO_TRI, "Đã tháo bộ nguồn", null, null,
						created.version()));
		MaintenanceResponse completed = maintenanceService.update(MANAGER_EMAIL, created.id(),
				new MaintenanceUpdateRequest(BaoTriTrangThai.HOAN_THANH, "Đã chạy kiểm thử", NOW,
						"Thiết bị hoạt động ổn định", active.version()));

		assertThat(completed.status()).isEqualTo(BaoTriTrangThai.HOAN_THANH);
		assertThat(completed.progress()).hasSize(3);
		assertThat(completed.progress()).extracting(item -> item.status()).containsExactly(BaoTriTrangThai.CHO_XU_LY,
				BaoTriTrangThai.DANG_BAO_TRI, BaoTriTrangThai.HOAN_THANH);
		assertThat(value("SELECT TrangThai FROM LichChan WHERE MaBaoTri = ?", String.class, created.id()))
				.isEqualTo("DA_HUY");
		assertThat(value("SELECT TrangThai FROM ThietBi WHERE MaThietBi = 'TB0001'", String.class))
				.isEqualTo("SAN_SANG");
		assertThat(value("SELECT KetQua FROM BaoTri WHERE MaBaoTri = ?", String.class, created.id()))
				.isEqualTo("Thiết bị hoạt động ổn định");
	}

	@Test
	void rejectsDifferentResourceAndReusingAnIncident() {
		insertIncident("S10-SC-SCOPE", "TN-TB0002");
		assertThatThrownBy(() -> maintenanceService.create(MANAGER_EMAIL,
				new MaintenanceCreateRequest("TN-TB0001", "S10-SC-SCOPE", "CB001", "Sai tài nguyên")))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

		MaintenanceResponse first = maintenanceService.create(MANAGER_EMAIL,
				new MaintenanceCreateRequest("TN-TB0002", "S10-SC-SCOPE", "CB001", "Kiểm tra nguồn"));
		MaintenanceResponse active = maintenanceService.update(MANAGER_EMAIL, first.id(), new MaintenanceUpdateRequest(
				BaoTriTrangThai.DANG_BAO_TRI, "Đang kiểm tra", null, null, first.version()));
		maintenanceService.update(MANAGER_EMAIL, first.id(), new MaintenanceUpdateRequest(BaoTriTrangThai.HOAN_THANH,
				"Hoàn tất", NOW, "Không phát hiện lỗi", active.version()));

		assertThatThrownBy(() -> maintenanceService.create(MANAGER_EMAIL,
				new MaintenanceCreateRequest("TN-TB0002", "S10-SC-SCOPE", "CB001", "Dùng lại sự cố")))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));
	}

	private void insertIncident(String id, String resourceId) {
		jdbcTemplate.update("""
				INSERT INTO SuCo
				  (MaSuCo, MaTaiNguyen, MaPhien, MaNguoiBao, MaNguoiXuLy, MucDo, MoTa, TrangThai, ThoiDiemBao)
				VALUES (?, ?, NULL, 'GV001', 'CB001', 'CAO', 'Sự cố kiểm thử bảo trì', 'DANG_XU_LY',
				        '2026-08-01 08:00:00')
				""", id, resourceId);
	}

	private <T> T value(String sql, Class<T> type, Object... arguments) {
		return jdbcTemplate.queryForObject(sql, type, arguments);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class FixedClockConfiguration {

		@Bean
		@Primary
		Clock maintenanceTestClock() {
			return Clock.fixed(NOW, ZoneOffset.UTC);
		}
	}
}
