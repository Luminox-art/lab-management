package com.example.labmanagement.usage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.incident.domain.MucDoSuCo;
import com.example.labmanagement.usage.domain.PhienSuDungTrangThai;
import com.example.labmanagement.usage.dto.SessionCheckInRequest;
import com.example.labmanagement.usage.dto.SessionCheckOutRequest;
import com.example.labmanagement.usage.dto.SessionDeviceConditionRequest;
import com.example.labmanagement.usage.dto.SessionIncidentRequest;
import com.example.labmanagement.usage.dto.UsageSessionResponse;
import com.example.labmanagement.usage.repository.PhienSuDungRepository;
import com.example.labmanagement.usage.service.SessionGenerationService;
import com.example.labmanagement.usage.service.UsageSessionService;
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
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(UsageSessionMySqlIntegrationTest.FixedClockConfiguration.class)
@EnabledIfEnvironmentVariable(named = "LAB_TEST_DB_PASSWORD", matches = ".+")
class UsageSessionMySqlIntegrationTest {

	private static final String ACTOR_EMAIL = "gv001@lab.local";
	private static final LocalDate TODAY = LocalDate.of(2026, 8, 1);

	@Autowired
	private SessionGenerationService generationService;

	@Autowired
	private UsageSessionService sessionService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private Clock clock;

	@Autowired
	private PhienSuDungRepository sessionRepository;

	@BeforeEach
	@AfterEach
	void cleanStageEightRows() {
		jdbcTemplate.update("""
				DELETE incident FROM SuCo incident
				JOIN PhienSuDung session ON session.MaPhien = incident.MaPhien
				JOIN LichDangKy schedule ON schedule.MaLich = session.MaLich
				WHERE schedule.MaPhieu LIKE 'S8-%'
				""");
		jdbcTemplate.update("""
				DELETE item FROM PhienSuDungThietBi item
				JOIN PhienSuDung session ON session.MaPhien = item.MaPhien
				JOIN LichDangKy schedule ON schedule.MaLich = session.MaLich
				WHERE schedule.MaPhieu LIKE 'S8-%'
				""");
		jdbcTemplate.update("""
				DELETE session FROM PhienSuDung session
				JOIN LichDangKy schedule ON schedule.MaLich = session.MaLich
				WHERE schedule.MaPhieu LIKE 'S8-%'
				""");
		jdbcTemplate.update("DELETE FROM PhieuDangKyThietBi WHERE MaPhieu LIKE 'S8-%'");
		jdbcTemplate.update("DELETE FROM LichDangKy WHERE MaPhieu LIKE 'S8-%'");
		jdbcTemplate.update("DELETE FROM PhieuDangKy WHERE MaPhieu LIKE 'S8-%'");
		jdbcTemplate.update("UPDATE ThietBi SET TrangThai = 'SAN_SANG' WHERE MaThietBi IN ('TB0001','TB0002')");
	}

	@Test
	void generatesIdempotentlyAndPersistsTheCompleteCheckInOutAggregate() {
		assertThat(clock.instant()).isEqualTo(Instant.parse("2026-08-01T01:00:00Z"));
		insertApprovedRegistration("S8-HAPPY", TODAY, 7, 2, "TB0001");

		assertThat(generationService.generateForRegistration("S8-HAPPY")).isEqualTo(1);
		assertThat(generationService.generateForRegistration("S8-HAPPY")).isZero();
		Long sessionId = value("""
				SELECT session.MaPhien FROM PhienSuDung session
				JOIN LichDangKy schedule ON schedule.MaLich = session.MaLich
				WHERE schedule.MaPhieu = 'S8-HAPPY'
				""", Long.class);
		UsageSessionResponse beforeCheckIn = sessionService.get(ACTOR_EMAIL, sessionId);
		assertThat(beforeCheckIn.usageDate()).isEqualTo(TODAY);
		assertThat(beforeCheckIn.startTime()).isEqualTo(java.time.LocalTime.of(7, 50));
		assertThat(beforeCheckIn.endTime()).isEqualTo(java.time.LocalTime.of(8, 40));
		assertThat(beforeCheckIn.canCheckIn()).isTrue();

		UsageSessionResponse checkedIn = sessionService.checkIn(ACTOR_EMAIL, sessionId, new SessionCheckInRequest(0L,
				List.of(new SessionDeviceConditionRequest("TB0001", "Tốt", "Đủ phụ kiện"))));
		assertThat(checkedIn.status()).isEqualTo(PhienSuDungTrangThai.DANG_SU_DUNG);
		assertThat(value("SELECT TrangThai FROM PhieuDangKy WHERE MaPhieu = 'S8-HAPPY'", String.class))
				.isEqualTo("DANG_SU_DUNG");
		assertThat(value("SELECT TrangThai FROM ThietBi WHERE MaThietBi = 'TB0001'", String.class))
				.isEqualTo("DANG_SU_DUNG");
		assertConflict(() -> sessionService.checkIn(ACTOR_EMAIL, sessionId,
				new SessionCheckInRequest(checkedIn.version(), List.of())));

		UsageSessionResponse checkedOut = sessionService.checkOut(ACTOR_EMAIL, sessionId,
				new SessionCheckOutRequest(checkedIn.version(),
						List.of(new SessionDeviceConditionRequest("TB0001", "Hỏng cổng nguồn", "Không khởi động")),
						List.of(new SessionIncidentRequest("TB0001", MucDoSuCo.CAO, "Cổng nguồn bị hỏng"))));

		assertThat(checkedOut.status()).isEqualTo(PhienSuDungTrangThai.HOAN_THANH);
		assertThat(checkedOut.incidentIds()).hasSize(1);
		assertThat(value("SELECT TrangThai FROM PhieuDangKy WHERE MaPhieu = 'S8-HAPPY'", String.class))
				.isEqualTo("HOAN_THANH");
		assertThat(value("SELECT TrangThai FROM ThietBi WHERE MaThietBi = 'TB0001'", String.class)).isEqualTo("HONG");
		assertThat(value("SELECT TinhTrangTra FROM PhienSuDungThietBi WHERE MaPhien = ? AND MaThietBi = 'TB0001'",
				String.class, sessionId)).isEqualTo("Hỏng cổng nguồn");
		assertThat(value("SELECT COUNT(*) FROM SuCo WHERE MaPhien = ? AND TrangThai = 'MOI'", Integer.class, sessionId))
				.isEqualTo(1);
		assertThat(sessionRepository.countActualUsageByRegistrationId("S8-HAPPY")).isEqualTo(1);
		assertConflict(() -> sessionService.checkOut(ACTOR_EMAIL, sessionId,
				new SessionCheckOutRequest(checkedOut.version(), List.of(), List.of())));
	}

	@Test
	void marksAnOverdueUnstartedSessionAbsentAndCompletesItsRegistration() {
		LocalDate yesterday = TODAY.minusDays(1);
		insertApprovedRegistration("S8-ABSENT", yesterday, 6, 1, null);
		assertThat(generationService.generateForRegistration("S8-ABSENT")).isEqualTo(1);

		assertThat(sessionService.markOverdueSessions()).isGreaterThanOrEqualTo(1);

		assertThat(value("""
				SELECT session.TrangThai FROM PhienSuDung session
				JOIN LichDangKy schedule ON schedule.MaLich = session.MaLich
				WHERE schedule.MaPhieu = 'S8-ABSENT'
				""", String.class)).isEqualTo("VANG_MAT");
		assertThat(value("SELECT TrangThai FROM PhieuDangKy WHERE MaPhieu = 'S8-ABSENT'", String.class))
				.isEqualTo("HOAN_THANH");
		assertThat(sessionRepository.countActualUsageByRegistrationId("S8-ABSENT")).isZero();
	}

	private void insertApprovedRegistration(String id, LocalDate date, int dayOfWeek, int periodId, String deviceId) {
		jdbcTemplate.update("""
				INSERT INTO PhieuDangKy
				  (MaPhieu, MaNguoiTao, MaPhong, LoaiPhieu, MucDich, SoNguoi, NgayBatDau, NgayKetThuc, TrangThai)
				VALUES (?, 'GV001', 'P0601', 'GIANG_DAY', 'S8 usage test', 10, ?, ?, 'DA_DUYET')
				""", id, date, date);
		jdbcTemplate.update("INSERT INTO LichDangKy (MaPhieu, Thu, MaTiet) VALUES (?, ?, ?)", id, dayOfWeek, periodId);
		if (deviceId != null) {
			jdbcTemplate.update("INSERT INTO PhieuDangKyThietBi (MaPhieu, MaThietBi, DaPhanBo) VALUES (?, ?, TRUE)", id,
					deviceId);
		}
	}

	private void assertConflict(ThrowingOperation operation) {
		assertThatThrownBy(operation::run).isInstanceOfSatisfying(ApiException.class,
				exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));
	}

	private <T> T value(String sql, Class<T> type, Object... arguments) {
		return jdbcTemplate.queryForObject(sql, type, arguments);
	}

	@FunctionalInterface
	private interface ThrowingOperation {
		void run();
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class FixedClockConfiguration {

		@Bean
		@Primary
		Clock usageTestClock() {
			return Clock.fixed(Instant.parse("2026-08-01T01:00:00Z"), ZoneOffset.UTC);
		}
	}
}
