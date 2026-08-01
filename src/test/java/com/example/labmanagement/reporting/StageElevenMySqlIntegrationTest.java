package com.example.labmanagement.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.labmanagement.notification.application.NotificationResponse;
import com.example.labmanagement.notification.application.NotificationService;
import com.example.labmanagement.notification.domain.NotificationType;
import com.example.labmanagement.reporting.application.DashboardFrequencyResponse;
import com.example.labmanagement.reporting.application.DashboardResponse;
import com.example.labmanagement.reporting.application.DashboardService;
import com.example.labmanagement.reporting.domain.DashboardGroup;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * TC-REP-01..10: scope notifications and independently reconcile actual-session
 * dashboard SQL.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(StageElevenMySqlIntegrationTest.FixedClockConfiguration.class)
@EnabledIfEnvironmentVariable(named = "LAB_TEST_DB_PASSWORD", matches = ".+")
class StageElevenMySqlIntegrationTest {

	private static final LocalDate DASHBOARD_FROM = LocalDate.of(2035, 2, 1);
	private static final LocalDate DASHBOARD_TO = LocalDate.of(2035, 2, 3);
	@Autowired
	private NotificationService notificationService;
	@Autowired
	private DashboardService dashboardService;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	@AfterEach
	void cleanStageElevenRows() {
		jdbcTemplate.update("DELETE FROM TienDoBaoTri WHERE MaBaoTri LIKE 'S11-%'");
		jdbcTemplate.update("DELETE FROM BaoTriSuCo WHERE MaBaoTri LIKE 'S11-%'");
		jdbcTemplate.update("DELETE FROM LichChan WHERE MaBaoTri LIKE 'S11-%'");
		jdbcTemplate.update("DELETE FROM BaoTri WHERE MaBaoTri LIKE 'S11-%'");
		jdbcTemplate.update("DELETE FROM SuCo WHERE MaSuCo LIKE 'S11-%'");
		jdbcTemplate.update("DELETE FROM XuLyPhieu WHERE MaPhieu LIKE 'S11-%'");
		jdbcTemplate.update("""
				DELETE usedDevice FROM PhienSuDungThietBi usedDevice
				JOIN PhienSuDung session ON session.MaPhien = usedDevice.MaPhien
				JOIN LichDangKy schedule ON schedule.MaLich = session.MaLich
				WHERE schedule.MaPhieu LIKE 'S11-%'
				""");
		jdbcTemplate.update("""
				DELETE session FROM PhienSuDung session
				JOIN LichDangKy schedule ON schedule.MaLich = session.MaLich
				WHERE schedule.MaPhieu LIKE 'S11-%'
				""");
		jdbcTemplate.update("DELETE FROM PhieuDangKyThietBi WHERE MaPhieu LIKE 'S11-%'");
		jdbcTemplate.update("DELETE FROM PhieuHuongDan WHERE MaPhieu LIKE 'S11-%'");
		jdbcTemplate.update("DELETE FROM LichDangKy WHERE MaPhieu LIKE 'S11-%'");
		jdbcTemplate.update("DELETE FROM PhieuDangKy WHERE MaPhieu LIKE 'S11-%'");
	}

	@Test
	void notificationProjectionIncludesFourKindsButDoesNotLeakAnotherUsersDecision() {
		insertRegistration("S11-OWN", "GV001", LocalDate.of(2035, 1, 16), 3, 1);
		insertRegistration("S11-OTHER", "GV002", LocalDate.of(2035, 1, 16), 4, 1);
		jdbcTemplate.update("""
				INSERT INTO XuLyPhieu (MaPhieu, MaNguoiXuLy, HanhDong, LyDo, ThoiDiem)
				VALUES ('S11-OWN','CB001','PHE_DUYET',NULL,'2035-01-15 02:00:00'),
				       ('S11-OTHER','CB001','TU_CHOI','Không phù hợp','2035-01-15 03:00:00')
				""");
		insertIncidentAndMaintenance();

		List<NotificationResponse> notifications = notificationService.notifications("gv001@lab.local", true, 0, 100)
				.getContent();

		assertThat(notifications).extracting(NotificationResponse::type).contains(NotificationType.PHIEU_DANG_KY,
				NotificationType.PHIEN_SAP_DIEN_RA, NotificationType.SU_CO, NotificationType.TIEN_DO_BAO_TRI);
		assertThat(notifications).extracting(NotificationResponse::content)
				.anyMatch(content -> content.contains("S11-OWN")).noneMatch(content -> content.contains("S11-OTHER"));
		assertThat(notifications).filteredOn(item -> item.type() == NotificationType.TIEN_DO_BAO_TRI)
				.extracting(NotificationResponse::targetUrl).contains("/incidents/S11-INC");
	}

	@Test
	void dashboardMatchesIndependentSqlAndExcludesAbsentSessionFromActualFrequency() {
		insertDashboardSessions();
		insertIncidentAndMaintenance();

		DashboardResponse byRoom = dashboardService.dashboard("cb001@lab.local", DASHBOARD_FROM, DASHBOARD_TO,
				DashboardGroup.PHONG);
		DashboardResponse byDevice = dashboardService.dashboard("cb001@lab.local", DASHBOARD_FROM, DASHBOARD_TO,
				DashboardGroup.THIET_BI);

		Long independentActual = value("""
				SELECT COUNT(*) FROM PhienSuDung
				WHERE NgaySuDung BETWEEN ? AND ?
				  AND TrangThai IN ('DANG_SU_DUNG','HOAN_THANH')
				""", Long.class, DASHBOARD_FROM, DASHBOARD_TO);
		Long independentRoomFrequency = value("""
				SELECT COUNT(*) FROM PhienSuDung session
				JOIN LichDangKy schedule ON schedule.MaLich = session.MaLich
				JOIN PhieuDangKy registration ON registration.MaPhieu = schedule.MaPhieu
				WHERE session.NgaySuDung BETWEEN ? AND ?
				  AND session.TrangThai IN ('DANG_SU_DUNG','HOAN_THANH')
				  AND registration.MaPhong = 'P0601'
				""", Long.class, DASHBOARD_FROM, DASHBOARD_TO);

		assertThat(byRoom.summary().actualSessions()).isEqualTo(independentActual).isEqualTo(2);
		assertThat(byRoom.summary().completedSessions()).isEqualTo(1);
		assertThat(byRoom.summary().absentSessions()).isEqualTo(1);
		assertThat(byRoom.frequencies()).filteredOn(item -> item.id().equals("P0601"))
				.extracting(DashboardFrequencyResponse::count).containsExactly(independentRoomFrequency);
		assertThat(byDevice.frequencies()).filteredOn(item -> item.id().equals("TB0001"))
				.extracting(DashboardFrequencyResponse::count).containsExactly(2L);
	}

	private void insertRegistration(String id, String creatorId, LocalDate usageDate, int dayOfWeek, int periodId) {
		jdbcTemplate.update("""
				INSERT INTO PhieuDangKy
				  (MaPhieu, MaNguoiTao, MaPhong, LoaiPhieu, MucDich, SoNguoi, NgayBatDau, NgayKetThuc, TrangThai)
				VALUES (?, ?, 'P0601', 'GIANG_DAY', 'S11 reporting test', 10, ?, ?, 'DA_DUYET')
				""", id, creatorId, usageDate, usageDate);
		jdbcTemplate.update("INSERT INTO LichDangKy (MaPhieu, Thu, MaTiet) VALUES (?, ?, ?)", id, dayOfWeek, periodId);
		Long scheduleId = value("SELECT MaLich FROM LichDangKy WHERE MaPhieu = ?", Long.class, id);
		jdbcTemplate.update("INSERT INTO PhienSuDung (MaLich, NgaySuDung, TrangThai) VALUES (?, ?, 'CHUA_BAT_DAU')",
				scheduleId, usageDate);
	}

	private void insertDashboardSessions() {
		jdbcTemplate.update("""
				INSERT INTO PhieuDangKy
				  (MaPhieu, MaNguoiTao, MaPhong, LoaiPhieu, MucDich, SoNguoi, NgayBatDau, NgayKetThuc, TrangThai)
				VALUES ('S11-DASH','GV001','P0601','GIANG_DAY','S11 dashboard test',10,?,?, 'HOAN_THANH')
				""", DASHBOARD_FROM, DASHBOARD_TO);
		jdbcTemplate.update("INSERT INTO LichDangKy (MaPhieu, Thu, MaTiet) VALUES ('S11-DASH', 5, 1)");
		Long scheduleId = value("SELECT MaLich FROM LichDangKy WHERE MaPhieu = 'S11-DASH'", Long.class);
		jdbcTemplate.update("""
				INSERT INTO PhienSuDung
				  (MaLich, NgaySuDung, TrangThai, ThoiDiemCheckIn, ThoiDiemCheckOut, MaNguoiCheckIn, MaNguoiCheckOut)
				VALUES (?, '2035-02-01', 'HOAN_THANH', '2035-02-01 01:00:00', '2035-02-01 02:00:00', 'GV001', 'GV001'),
				       (?, '2035-02-02', 'DANG_SU_DUNG', '2035-02-02 01:00:00', NULL, 'GV001', NULL),
				       (?, '2035-02-03', 'VANG_MAT', NULL, NULL, NULL, NULL)
				""", scheduleId, scheduleId, scheduleId);
		List<Long> actualSessionIds = jdbcTemplate.queryForList("""
				SELECT MaPhien FROM PhienSuDung
				WHERE MaLich = ? AND TrangThai IN ('DANG_SU_DUNG','HOAN_THANH')
				""", Long.class, scheduleId);
		actualSessionIds.forEach(sessionId -> jdbcTemplate.update("""
				INSERT INTO PhienSuDungThietBi (MaPhien, MaThietBi, TinhTrangNhan)
				VALUES (?, 'TB0001', 'Tốt')
				""", sessionId));
	}

	private void insertIncidentAndMaintenance() {
		jdbcTemplate.update("""
				INSERT INTO SuCo
				  (MaSuCo, MaTaiNguyen, MaNguoiBao, MaNguoiXuLy, MucDo, MoTa, TrangThai, ThoiDiemBao)
				VALUES ('S11-INC','TN-TB0001','GV001','CB001','CAO','Mất nguồn','DANG_XU_LY',
				        '2035-02-02 02:00:00')
				""");
		jdbcTemplate.update("""
				INSERT INTO BaoTri
				  (MaBaoTri, MaTaiNguyen, MaNguoiPhuTrach, NgayBatDau, NoiDung, TrangThai)
				VALUES ('S11-BT','TN-TB0001','CB001','2035-02-02 03:00:00','Thay nguồn','DANG_BAO_TRI')
				""");
		jdbcTemplate.update("INSERT INTO BaoTriSuCo (MaBaoTri, MaSuCo) VALUES ('S11-BT','S11-INC')");
		jdbcTemplate.update("""
				INSERT INTO TienDoBaoTri (MaBaoTri, ThoiDiem, TrangThai, NoiDung, MaNguoiCapNhat)
				VALUES ('S11-BT','2035-02-02 04:00:00','DANG_BAO_TRI','Đang thay nguồn','CB001')
				""");
	}

	private <T> T value(String sql, Class<T> type, Object... arguments) {
		return jdbcTemplate.queryForObject(sql, type, arguments);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class FixedClockConfiguration {

		@Bean
		@Primary
		Clock stageElevenClock() {
			return Clock.fixed(Instant.parse("2035-01-15T02:00:00Z"), ZoneOffset.UTC);
		}
	}
}
