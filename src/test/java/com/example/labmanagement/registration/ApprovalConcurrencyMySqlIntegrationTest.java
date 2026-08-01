package com.example.labmanagement.registration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.labmanagement.catalog.application.CatalogService;
import com.example.labmanagement.catalog.application.DeviceUpdateRequest;
import com.example.labmanagement.catalog.domain.ThietBiTrangThai;
import com.example.labmanagement.catalog.persistence.TaiNguyenRepository;
import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.registration.application.ApprovalRequest;
import com.example.labmanagement.registration.application.ApprovalService;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** CON-01..06 repeated on independent MySQL connections. */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "LAB_TEST_DB_PASSWORD", matches = ".+")
class ApprovalConcurrencyMySqlIntegrationTest {

	private static final int REPEAT_COUNT = 20;
	private static final String MANAGER_EMAIL = "cb001@lab.local";
	private static final LocalDate FIRST_MONDAY = LocalDate.of(2035, 1, 1);

	@Autowired
	private ApprovalService approvalService;

	@Autowired
	private CatalogService catalogService;

	@Autowired
	private TaiNguyenRepository resourceRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	@AfterEach
	void cleanConcurrencyRows() {
		jdbcTemplate.update("DELETE FROM LichChan WHERE LyDo LIKE 'S6C-%'");
		jdbcTemplate.update("DELETE FROM XuLyPhieu WHERE MaPhieu LIKE 'S6C-%'");
		jdbcTemplate.update("DELETE FROM PhieuDangKyThietBi WHERE MaPhieu LIKE 'S6C-%'");
		jdbcTemplate.update("DELETE FROM LichDangKy WHERE MaPhieu LIKE 'S6C-%'");
		jdbcTemplate.update("DELETE FROM PhieuDangKy WHERE MaPhieu LIKE 'S6C-%'");
		jdbcTemplate
				.update("UPDATE ThietBi SET TrangThai = 'SAN_SANG' WHERE MaThietBi IN ('TB0002','TB0003','TB0004')");
	}

	@Test
	void sameRoomSameSlotHasExactlyOneWinnerInEveryRace() throws Exception {
		for (int iteration = 0; iteration < REPEAT_COUNT; iteration++) {
			String firstId = id("ROOM", iteration, "A");
			String secondId = id("ROOM", iteration, "B");
			LocalDate date = FIRST_MONDAY.plusWeeks(iteration);
			insertRegistration(firstId, "P0601", date, List.of());
			insertRegistration(secondId, "P0601", date, List.of());

			RaceResult result = race(approve(firstId, List.of()), approve(secondId, List.of()));

			assertThat(result.successCount()).as("same-room iteration %s", iteration).isEqualTo(1);
			assertOneApproved(firstId, secondId);
		}
	}

	@Test
	void sameMobileDeviceAcrossRoomsHasExactlyOneWinnerInEveryRace() throws Exception {
		for (int iteration = 0; iteration < REPEAT_COUNT; iteration++) {
			String firstId = id("DEVICE", iteration, "A");
			String secondId = id("DEVICE", iteration, "B");
			LocalDate date = FIRST_MONDAY.plusWeeks(iteration);
			insertRegistration(firstId, "P0601", date, List.of("TB0002"));
			insertRegistration(secondId, "P0602", date, List.of("TB0002"));

			RaceResult result = race(approve(firstId, List.of("TB0002")), approve(secondId, List.of("TB0002")));

			assertThat(result.successCount()).as("same-device iteration %s", iteration).isEqualTo(1);
			assertOneApproved(firstId, secondId);
			assertThat(count("""
					SELECT COUNT(*) FROM PhieuDangKyThietBi allocation
					JOIN PhieuDangKy registration ON registration.MaPhieu = allocation.MaPhieu
					WHERE allocation.MaThietBi = 'TB0002' AND allocation.DaPhanBo = TRUE
					  AND registration.MaPhieu IN (?, ?)
					""", firstId, secondId)).isEqualTo(1);
		}
	}

	@Test
	void approvalAndBlockCreationCannotCoexistInEveryRace() throws Exception {
		for (int iteration = 0; iteration < REPEAT_COUNT; iteration++) {
			String registrationId = id("BLOCK", iteration, "A");
			LocalDate date = FIRST_MONDAY.plusWeeks(iteration);
			insertRegistration(registrationId, "P0603", date, List.of());

			RaceResult result = race(approve(registrationId, List.of()), createRoomBlock(date, iteration));

			assertThat(result.successCount()).as("approve-vs-block iteration %s", iteration).isEqualTo(1);
			int approved = count("SELECT COUNT(*) FROM PhieuDangKy WHERE MaPhieu = ? AND TrangThai = 'DA_DUYET'",
					registrationId);
			int blocked = count("SELECT COUNT(*) FROM LichChan WHERE LyDo = ?", "S6C-BLOCK-" + iteration);
			assertThat(approved + blocked).isEqualTo(1);
		}
	}

	@Test
	void approvalAndDeviceMaintenanceCannotBothWinInEveryRace() throws Exception {
		for (int iteration = 0; iteration < REPEAT_COUNT; iteration++) {
			String registrationId = id("MAINT", iteration, "A");
			LocalDate date = FIRST_MONDAY.plusWeeks(iteration);
			insertRegistration(registrationId, "P0601", date, List.of("TB0004"));
			long deviceVersion = value("SELECT VersionNo FROM ThietBi WHERE MaThietBi = 'TB0004'", Long.class);

			RaceResult result = race(approve(registrationId, List.of("TB0004")), maintainDevice(deviceVersion));

			assertThat(result.successCount()).as("approve-vs-maintenance iteration %s", iteration).isEqualTo(1);
			int approvedAllocation = count("""
					SELECT COUNT(*) FROM PhieuDangKy registration
					JOIN PhieuDangKyThietBi allocation ON allocation.MaPhieu = registration.MaPhieu
					WHERE registration.MaPhieu = ? AND registration.TrangThai = 'DA_DUYET'
					  AND allocation.MaThietBi = 'TB0004' AND allocation.DaPhanBo = TRUE
					""", registrationId);
			String deviceStatus = value("SELECT TrangThai FROM ThietBi WHERE MaThietBi = 'TB0004'", String.class);
			assertThat(approvedAllocation == 1 && "BAO_TRI".equals(deviceStatus)).isFalse();
			deleteRegistration(registrationId);
			jdbcTemplate.update("UPDATE ThietBi SET TrangThai = 'SAN_SANG' WHERE MaThietBi = 'TB0004'");
		}
	}

	@Test
	void retryingTheSameApprovalCreatesAtMostOneHistoryRowInEveryRace() throws Exception {
		for (int iteration = 0; iteration < REPEAT_COUNT; iteration++) {
			String registrationId = id("RETRY", iteration, "A");
			insertRegistration(registrationId, "P0601", FIRST_MONDAY.plusWeeks(iteration), List.of());

			RaceResult result = race(approve(registrationId, List.of()), approve(registrationId, List.of()));

			assertThat(result.successCount()).as("retry iteration %s", iteration).isEqualTo(1);
			assertThat(count("SELECT COUNT(*) FROM XuLyPhieu WHERE MaPhieu = ? AND HanhDong = 'PHE_DUYET'",
					registrationId)).isEqualTo(1);
		}
	}

	@Test
	void reverseDeviceInputOrderNeverDeadlocksOrPartiallyAllocates() throws Exception {
		for (int iteration = 0; iteration < REPEAT_COUNT; iteration++) {
			String firstId = id("ORDER", iteration, "A");
			String secondId = id("ORDER", iteration, "B");
			LocalDate date = FIRST_MONDAY.plusWeeks(iteration);
			insertRegistration(firstId, "P0601", date, List.of("TB0002", "TB0003"));
			insertRegistration(secondId, "P0602", date, List.of("TB0002", "TB0003"));

			RaceResult result = race(approve(firstId, List.of("TB0002", "TB0003")),
					approve(secondId, List.of("TB0003", "TB0002")));

			assertThat(result.successCount()).as("device-order iteration %s", iteration).isEqualTo(1);
			assertOneApproved(firstId, secondId);
			List<Integer> allocationCounts = jdbcTemplate.query("""
					SELECT SUM(DaPhanBo) allocatedCount
					FROM PhieuDangKyThietBi
					WHERE MaPhieu IN (?, ?)
					GROUP BY MaPhieu
					ORDER BY MaPhieu
					""", (resultSet, rowNumber) -> resultSet.getInt("allocatedCount"), firstId, secondId);
			assertThat(allocationCounts).containsExactlyInAnyOrder(0, 2);
		}
	}

	private Callable<Boolean> approve(String registrationId, List<String> deviceIds) {
		return () -> {
			try {
				approvalService.approve(MANAGER_EMAIL, registrationId, new ApprovalRequest(deviceIds, 0L));
				return true;
			} catch (ApiException exception) {
				assertThat(exception.getStatus().value()).isEqualTo(409);
				return false;
			}
		};
	}

	private Callable<Boolean> createRoomBlock(LocalDate date, int iteration) {
		return () -> readCommittedTemplate().execute(status -> {
			resourceRepository.lockForApproval("P0603", List.of("__NO_SELECTED_DEVICE__"));
			int approved = count("""
					SELECT COUNT(*) FROM PhieuDangKy registration
					JOIN LichDangKy schedule ON schedule.MaPhieu = registration.MaPhieu
					WHERE registration.MaPhong = 'P0603' AND registration.TrangThai IN ('DA_DUYET','DANG_SU_DUNG')
					  AND registration.NgayBatDau <= ? AND registration.NgayKetThuc >= ?
					  AND schedule.Thu = 2 AND schedule.MaTiet = 1
					""", date, date);
			if (approved > 0) {
				return false;
			}
			jdbcTemplate.update("""
					INSERT INTO LichChan
					  (MaTaiNguyen, NgayBatDau, NgayKetThuc, Thu, MaTiet, LyDo, TrangThai, MaNguoiTao)
					VALUES ('TN-P0603', ?, ?, 2, 1, ?, 'HIEU_LUC', 'CB001')
					""", date, date, "S6C-BLOCK-" + iteration);
			return true;
		});
	}

	private Callable<Boolean> maintainDevice(long version) {
		return () -> {
			try {
				catalogService.updateDevice("TB0004", new DeviceUpdateRequest("Thiết bị demo 0004", "ROBOT",
						"SERIAL-000004", "MODEL-04", "P0604", ThietBiTrangThai.BAO_TRI, version));
				return true;
			} catch (ApiException exception) {
				assertThat(exception.getStatus().value()).isEqualTo(409);
				return false;
			}
		};
	}

	private RaceResult race(Callable<Boolean> first, Callable<Boolean> second) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		try {
			Future<Boolean> firstFuture = executor.submit(awaitStart(first, ready, start));
			Future<Boolean> secondFuture = executor.submit(awaitStart(second, ready, start));
			assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
			start.countDown();
			return new RaceResult(firstFuture.get(10, TimeUnit.SECONDS), secondFuture.get(10, TimeUnit.SECONDS));
		} finally {
			executor.shutdownNow();
			assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
		}
	}

	private Callable<Boolean> awaitStart(Callable<Boolean> operation, CountDownLatch ready, CountDownLatch start) {
		return () -> {
			ready.countDown();
			assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
			return operation.call();
		};
	}

	private TransactionTemplate readCommittedTemplate() {
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
		return template;
	}

	private void assertOneApproved(String firstId, String secondId) {
		assertThat(count("""
				SELECT COUNT(*) FROM PhieuDangKy
				WHERE MaPhieu IN (?, ?) AND TrangThai = 'DA_DUYET'
				""", firstId, secondId)).isEqualTo(1);
		assertThat(count("""
				SELECT COUNT(*) FROM XuLyPhieu
				WHERE MaPhieu IN (?, ?) AND HanhDong = 'PHE_DUYET'
				""", firstId, secondId)).isEqualTo(1);
	}

	private void insertRegistration(String id, String roomId, LocalDate date, List<String> deviceIds) {
		jdbcTemplate.update("""
				INSERT INTO PhieuDangKy
				  (MaPhieu, MaNguoiTao, MaPhong, LoaiPhieu, MucDich, SoNguoi, NgayBatDau, NgayKetThuc, TrangThai)
				VALUES (?, 'GV001', ?, 'NGHIEN_CUU', 'S6C concurrency', 10, ?, ?, 'CHO_DUYET')
				""", id, roomId, date, date);
		jdbcTemplate.update("INSERT INTO LichDangKy (MaPhieu, Thu, MaTiet) VALUES (?, 2, 1)", id);
		for (String deviceId : deviceIds) {
			jdbcTemplate.update("INSERT INTO PhieuDangKyThietBi (MaPhieu, MaThietBi, DaPhanBo) VALUES (?, ?, FALSE)",
					id, deviceId);
		}
	}

	private void deleteRegistration(String id) {
		jdbcTemplate.update("DELETE FROM XuLyPhieu WHERE MaPhieu = ?", id);
		jdbcTemplate.update("DELETE FROM PhieuDangKyThietBi WHERE MaPhieu = ?", id);
		jdbcTemplate.update("DELETE FROM LichDangKy WHERE MaPhieu = ?", id);
		jdbcTemplate.update("DELETE FROM PhieuDangKy WHERE MaPhieu = ?", id);
	}

	private String id(String category, int iteration, String suffix) {
		return "S6C-" + category + "-" + iteration + "-" + suffix;
	}

	private int count(String sql, Object... arguments) {
		return jdbcTemplate.queryForObject(sql, Integer.class, arguments);
	}

	private <T> T value(String sql, Class<T> type, Object... arguments) {
		return jdbcTemplate.queryForObject(sql, type, arguments);
	}

	private record RaceResult(boolean firstSucceeded, boolean secondSucceeded) {
		int successCount() {
			return (firstSucceeded ? 1 : 0) + (secondSucceeded ? 1 : 0);
		}
	}
}
