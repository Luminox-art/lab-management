package com.example.labmanagement.registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.registration.domain.LoaiPhieu;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import com.example.labmanagement.registration.dto.ApprovalRequest;
import com.example.labmanagement.registration.dto.RegistrationDecisionResponse;
import com.example.labmanagement.registration.dto.RejectionRequest;
import com.example.labmanagement.registration.service.ApprovalService;
import com.example.labmanagement.registration.service.RegistrationService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** MySQL acceptance for API-22/23, FR-15/16 and TC-APR-10..20. */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "LAB_TEST_DB_PASSWORD", matches = ".+")
class ApprovalMySqlIntegrationTest {

	private static final String MANAGER_EMAIL = "cb001@lab.local";
	private static final LocalDate FIRST_MONDAY = LocalDate.of(2035, 1, 1);

	@Autowired
	private ApprovalService approvalService;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	@AfterEach
	void cleanStageSixRows() {
		jdbcTemplate.update("DELETE FROM LichChan WHERE LyDo LIKE 'S6-%'");
		jdbcTemplate.update("DELETE FROM XuLyPhieu WHERE MaPhieu LIKE 'S6-%'");
		jdbcTemplate.update("""
				DELETE session FROM PhienSuDung session
				JOIN LichDangKy schedule ON schedule.MaLich = session.MaLich
				WHERE schedule.MaPhieu LIKE 'S6-%'
				""");
		jdbcTemplate.update("DELETE FROM PhieuHuongDan WHERE MaPhieu LIKE 'S6-%'");
		jdbcTemplate.update("DELETE FROM PhieuDangKyThietBi WHERE MaPhieu LIKE 'S6-%'");
		jdbcTemplate.update("DELETE FROM LichDangKy WHERE MaPhieu LIKE 'S6-%'");
		jdbcTemplate.update("DELETE FROM PhieuDangKy WHERE MaPhieu LIKE 'S6-%'");
		jdbcTemplate.update("UPDATE Phong SET TrangThai = 'SAN_SANG' WHERE MaPhong IN ('P0601','P0602','P0603')");
		jdbcTemplate
				.update("UPDATE ThietBi SET TrangThai = 'SAN_SANG' WHERE MaThietBi IN ('TB0001','TB0002','TB0004')");
	}

	@Test
	void approvesAtomicallyWithActualAllocationAndSingleHistory() {
		insertRegistration("S6-HAPPY", "GV001", "P0601", 20, FIRST_MONDAY, 2, 1, List.of("TB0001"));

		RegistrationDecisionResponse result = approvalService.approve(MANAGER_EMAIL, "S6-HAPPY",
				new ApprovalRequest(List.of("TB0001"), 0L));

		assertThat(result.status()).isEqualTo(PhieuDangKyTrangThai.DA_DUYET);
		assertThat(result.version()).isEqualTo(1);
		assertThat(value("SELECT TrangThai FROM PhieuDangKy WHERE MaPhieu = 'S6-HAPPY'", String.class))
				.isEqualTo("DA_DUYET");
		assertThat(value("SELECT DaPhanBo FROM PhieuDangKyThietBi WHERE MaPhieu = 'S6-HAPPY'", Boolean.class)).isTrue();
		assertThat(count("SELECT COUNT(*) FROM XuLyPhieu WHERE MaPhieu = 'S6-HAPPY' AND HanhDong = 'PHE_DUYET'"))
				.isEqualTo(1);
	}

	@Test
	void capacityRoomDeviceAndSupervisorFailuresRollbackAllWrites() {
		insertRegistration("S6-CAPACITY", "GV001", "P0601", 41, FIRST_MONDAY, 2, 1, List.of());
		assertDecisionFailure("S6-CAPACITY", List.of(), 422, "sức chứa");

		jdbcTemplate.update("UPDATE Phong SET TrangThai = 'BAO_TRI' WHERE MaPhong = 'P0603'");
		insertRegistration("S6-ROOM-STATUS", "GV001", "P0603", 10, FIRST_MONDAY.plusWeeks(1), 2, 1, List.of());
		assertDecisionFailure("S6-ROOM-STATUS", List.of(), 409, "BAO_TRI");

		jdbcTemplate.update("UPDATE ThietBi SET TrangThai = 'HONG' WHERE MaThietBi = 'TB0001'");
		insertRegistration("S6-DEVICE-STATUS", "GV001", "P0601", 10, FIRST_MONDAY.plusWeeks(2), 2, 1,
				List.of("TB0001"));
		assertDecisionFailure("S6-DEVICE-STATUS", List.of("TB0001"), 409, "HONG");

		insertRegistration("S6-SUPERVISOR", "SV001", "P0601", 10, FIRST_MONDAY.plusWeeks(3), 2, 1, List.of("TB0004"));
		assertDecisionFailure("S6-SUPERVISOR", List.of("TB0004"), 422, "giảng viên hướng dẫn");
	}

	@Test
	void roomDeviceAndAllDayBlockConflictsCannotLeakPersonalData() {
		insertRegistration("S6-ROOM-A", "GV001", "P0601", 10, FIRST_MONDAY, 2, 1, List.of());
		insertRegistration("S6-ROOM-B", "GV002", "P0601", 10, FIRST_MONDAY, 2, 1, List.of());
		approvalService.approve(MANAGER_EMAIL, "S6-ROOM-A", new ApprovalRequest(List.of(), 0L));
		assertPrivateConflict("S6-ROOM-B", List.of());

		LocalDate deviceDate = FIRST_MONDAY.plusWeeks(4);
		insertRegistration("S6-DEVICE-A", "GV001", "P0601", 10, deviceDate, 2, 1, List.of("TB0002"));
		insertRegistration("S6-DEVICE-B", "GV002", "P0602", 10, deviceDate, 2, 1, List.of("TB0002"));
		approvalService.approve(MANAGER_EMAIL, "S6-DEVICE-A", new ApprovalRequest(List.of("TB0002"), 0L));
		assertPrivateConflict("S6-DEVICE-B", List.of("TB0002"));

		LocalDate blockedDate = FIRST_MONDAY.plusWeeks(5);
		jdbcTemplate.update("""
				INSERT INTO LichChan
				  (MaTaiNguyen, NgayBatDau, NgayKetThuc, Thu, MaTiet, LyDo, TrangThai, MaNguoiTao)
				VALUES ('TN-P0603', ?, ?, NULL, NULL, 'S6-all-day-block', 'HIEU_LUC', 'CB001')
				""", blockedDate, blockedDate);
		insertRegistration("S6-BLOCKED", "GV001", "P0603", 10, blockedDate, 2, 1, List.of());
		assertDecisionFailure("S6-BLOCKED", List.of(), 409, "Lịch chặn cả ngày");
	}

	@Test
	void staleVersionRetryAndRejectReasonAreEnforced() {
		insertRegistration("S6-STALE", "GV001", "P0601", 10, FIRST_MONDAY, 2, 1, List.of());
		assertThatThrownBy(
				() -> approvalService.approve(MANAGER_EMAIL, "S6-STALE", new ApprovalRequest(List.of(), 99L)))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getStatus().value()).isEqualTo(409));

		RegistrationDecisionResponse rejected = approvalService.reject(MANAGER_EMAIL, "S6-STALE",
				new RejectionRequest("  Không đáp ứng điều kiện an toàn  ", 0L));
		assertThat(rejected.status()).isEqualTo(PhieuDangKyTrangThai.TU_CHOI);
		assertThat(value("SELECT LyDo FROM XuLyPhieu WHERE MaPhieu = 'S6-STALE'", String.class))
				.isEqualTo("Không đáp ứng điều kiện an toàn");
		assertThatThrownBy(() -> approvalService.reject(MANAGER_EMAIL, "S6-STALE",
				new RejectionRequest("Gửi lại", rejected.version()))).isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getStatus().value()).isEqualTo(409));
		assertThat(count("SELECT COUNT(*) FROM XuLyPhieu WHERE MaPhieu = 'S6-STALE'")).isEqualTo(1);
	}

	@Test
	void managerQueueFiltersByRoomDateCreatorAndOnlyShowsPending() {
		LocalDate filterDate = FIRST_MONDAY.plusWeeks(8);
		insertRegistration("S6-QUEUE-A", "GV002", "P0603", 10, filterDate, 2, 1, List.of());
		insertRegistration("S6-QUEUE-B", "GV001", "P0601", 10, filterDate, 2, 1, List.of());

		var result = registrationService.search(MANAGER_EMAIL, LoaiPhieu.NGHIEN_CUU, PhieuDangKyTrangThai.DA_DUYET,
				"P0603", filterDate, "GV002", 0, 20);

		assertThat(result.getContent()).extracting(item -> item.id()).containsExactly("S6-QUEUE-A");
		assertThat(result.getContent()).allSatisfy(item -> {
			assertThat(item.status()).isEqualTo(PhieuDangKyTrangThai.CHO_DUYET);
			assertThat(item.warnings()).isNotNull();
		});
	}

	private void assertDecisionFailure(String registrationId, List<String> deviceIds, int status, String message) {
		assertThatThrownBy(
				() -> approvalService.approve(MANAGER_EMAIL, registrationId, new ApprovalRequest(deviceIds, 0L)))
				.isInstanceOfSatisfying(ApiException.class, exception -> {
					assertThat(exception.getStatus().value()).isEqualTo(status);
					assertThat(exception.getMessage()).containsIgnoringCase(message);
				});
		assertThat(value("SELECT TrangThai FROM PhieuDangKy WHERE MaPhieu = ?", String.class, registrationId))
				.isEqualTo("CHO_DUYET");
		assertThat(count("SELECT COUNT(*) FROM XuLyPhieu WHERE MaPhieu = ?", registrationId)).isZero();
		assertThat(
				count("SELECT COUNT(*) FROM PhieuDangKyThietBi WHERE MaPhieu = ? AND DaPhanBo = TRUE", registrationId))
				.isZero();
	}

	private void assertPrivateConflict(String registrationId, List<String> deviceIds) {
		assertThatThrownBy(
				() -> approvalService.approve(MANAGER_EMAIL, registrationId, new ApprovalRequest(deviceIds, 0L)))
				.isInstanceOfSatisfying(ApiException.class, exception -> {
					assertThat(exception.getStatus().value()).isEqualTo(409);
					assertThat(exception.getMessage()).doesNotContain("S6-", "GV001", "GV002", "@lab.local");
				});
		assertThat(count("SELECT COUNT(*) FROM XuLyPhieu WHERE MaPhieu = ?", registrationId)).isZero();
	}

	private void insertRegistration(String id, String creatorId, String roomId, int attendees, LocalDate date,
			int dayOfWeek, int periodId, List<String> deviceIds) {
		jdbcTemplate.update("""
				INSERT INTO PhieuDangKy
				  (MaPhieu, MaNguoiTao, MaPhong, LoaiPhieu, MucDich, SoNguoi, NgayBatDau, NgayKetThuc, TrangThai)
				VALUES (?, ?, ?, 'NGHIEN_CUU', 'S6 kiểm thử phê duyệt', ?, ?, ?, 'CHO_DUYET')
				""", id, creatorId, roomId, attendees, date, date);
		jdbcTemplate.update("INSERT INTO LichDangKy (MaPhieu, Thu, MaTiet) VALUES (?, ?, ?)", id, dayOfWeek, periodId);
		for (String deviceId : deviceIds) {
			jdbcTemplate.update("INSERT INTO PhieuDangKyThietBi (MaPhieu, MaThietBi, DaPhanBo) VALUES (?, ?, FALSE)",
					id, deviceId);
		}
	}

	private int count(String sql, Object... arguments) {
		return jdbcTemplate.queryForObject(sql, Integer.class, arguments);
	}

	private <T> T value(String sql, Class<T> type, Object... arguments) {
		return jdbcTemplate.queryForObject(sql, type, arguments);
	}
}
