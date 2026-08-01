package com.example.labmanagement.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.incident.application.IncidentCreateRequest;
import com.example.labmanagement.incident.application.IncidentResponse;
import com.example.labmanagement.incident.application.IncidentService;
import com.example.labmanagement.incident.application.IncidentUpdateRequest;
import com.example.labmanagement.incident.domain.MucDoSuCo;
import com.example.labmanagement.incident.domain.SuCoTrangThai;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
@Import(IncidentMySqlIntegrationTest.FixedClockConfiguration.class)
@EnabledIfEnvironmentVariable(named = "LAB_TEST_DB_PASSWORD", matches = ".+")
class IncidentMySqlIntegrationTest {

	private static final String REPORTER_EMAIL = "gv001@lab.local";
	private static final String MANAGER_EMAIL = "cb001@lab.local";

	@Autowired
	private IncidentService incidentService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	@AfterEach
	void cleanStageNineRows() {
		jdbcTemplate.update("""
				DELETE incident FROM SuCo incident
				JOIN PhienSuDung session ON session.MaPhien = incident.MaPhien
				JOIN LichDangKy schedule ON schedule.MaLich = session.MaLich
				WHERE schedule.MaPhieu LIKE 'S9-%'
				""");
		jdbcTemplate.update("""
				DELETE session FROM PhienSuDung session
				JOIN LichDangKy schedule ON schedule.MaLich = session.MaLich
				WHERE schedule.MaPhieu LIKE 'S9-%'
				""");
		jdbcTemplate.update("DELETE FROM PhieuDangKyThietBi WHERE MaPhieu LIKE 'S9-%'");
		jdbcTemplate.update("DELETE FROM LichDangKy WHERE MaPhieu LIKE 'S9-%'");
		jdbcTemplate.update("DELETE FROM PhieuDangKy WHERE MaPhieu LIKE 'S9-%'");
		jdbcTemplate.update("UPDATE ThietBi SET TrangThai = 'SAN_SANG' WHERE MaThietBi IN ('TB0001','TB0002')");
	}

	@Test
	void reportsAcceptsAndCompletesAnIncidentInOneConsistentLifecycle() {
		Long sessionId = insertSession("S9-HAPPY", "TB0001");

		IncidentResponse reported = incidentService.report(REPORTER_EMAIL,
				new IncidentCreateRequest("TN-TB0001", sessionId, MucDoSuCo.CAO, "Không khởi động"));
		assertThat(reported.status()).isEqualTo(SuCoTrangThai.MOI);
		assertThat(value("SELECT TrangThai FROM ThietBi WHERE MaThietBi = 'TB0001'", String.class)).isEqualTo("HONG");

		IncidentResponse accepted = incidentService.update(MANAGER_EMAIL, reported.id(),
				new IncidentUpdateRequest("CB001", SuCoTrangThai.DANG_XU_LY, null, reported.version()));
		IncidentResponse completed = incidentService.update(MANAGER_EMAIL, reported.id(),
				new IncidentUpdateRequest("CB001", SuCoTrangThai.DA_XU_LY, "Đã thay bộ nguồn", accepted.version()));

		assertThat(completed.status()).isEqualTo(SuCoTrangThai.DA_XU_LY);
		assertThat(completed.completedAt().toInstant()).isEqualTo(Instant.parse("2026-08-01T02:00:00Z"));
		assertThat(value("SELECT MaNguoiXuLy FROM SuCo WHERE MaSuCo = ?", String.class, reported.id()))
				.isEqualTo("CB001");
		assertThat(value("SELECT KetQua FROM SuCo WHERE MaSuCo = ?", String.class, reported.id()))
				.isEqualTo("Đã thay bộ nguồn");
		assertThat(incidentService.search(REPORTER_EMAIL, null, null, null, sessionId, null, 0, 20).getContent())
				.extracting(IncidentResponse::id).contains(reported.id());
	}

	@Test
	void refusesToAttachAnUnallocatedDeviceToTheSession() {
		Long sessionId = insertSession("S9-SCOPE", "TB0001");

		assertThatThrownBy(() -> incidentService.report(REPORTER_EMAIL,
				new IncidentCreateRequest("TN-TB0002", sessionId, MucDoSuCo.TRUNG_BINH, "Sai phạm vi")))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
		assertThat(value("SELECT COUNT(*) FROM SuCo WHERE MaPhien = ?", Integer.class, sessionId)).isZero();
	}

	private Long insertSession(String registrationId, String deviceId) {
		LocalDate date = LocalDate.of(2026, 8, 1);
		jdbcTemplate.update("""
				INSERT INTO PhieuDangKy
				  (MaPhieu, MaNguoiTao, MaPhong, LoaiPhieu, MucDich, SoNguoi, NgayBatDau, NgayKetThuc, TrangThai)
				VALUES (?, 'GV001', 'P0601', 'GIANG_DAY', 'S9 incident test', 10, ?, ?, 'HOAN_THANH')
				""", registrationId, date, date);
		jdbcTemplate.update("INSERT INTO LichDangKy (MaPhieu, Thu, MaTiet) VALUES (?, 7, 1)", registrationId);
		jdbcTemplate.update("INSERT INTO PhieuDangKyThietBi (MaPhieu, MaThietBi, DaPhanBo) VALUES (?, ?, TRUE)",
				registrationId, deviceId);
		Long scheduleId = value("SELECT MaLich FROM LichDangKy WHERE MaPhieu = ?", Long.class, registrationId);
		jdbcTemplate.update("""
				INSERT INTO PhienSuDung
				  (MaLich, NgaySuDung, TrangThai, ThoiDiemCheckIn, ThoiDiemCheckOut, MaNguoiCheckIn, MaNguoiCheckOut)
				VALUES (?, ?, 'HOAN_THANH', '2026-08-01 08:00:00', '2026-08-01 09:00:00', 'GV001', 'GV001')
				""", scheduleId, date);
		return value("SELECT MaPhien FROM PhienSuDung WHERE MaLich = ?", Long.class, scheduleId);
	}

	private <T> T value(String sql, Class<T> type, Object... arguments) {
		return jdbcTemplate.queryForObject(sql, type, arguments);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class FixedClockConfiguration {

		@Bean
		@Primary
		Clock incidentTestClock() {
			return Clock.fixed(Instant.parse("2026-08-01T02:00:00Z"), ZoneOffset.UTC);
		}
	}
}
