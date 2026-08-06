package com.example.labmanagement.registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.registration.domain.LoaiPhieu;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import com.example.labmanagement.registration.dto.RegistrationCancelRequest;
import com.example.labmanagement.registration.dto.RegistrationFormRequest;
import com.example.labmanagement.registration.dto.RegistrationResponse;
import com.example.labmanagement.registration.dto.RegistrationScheduleRequest;
import com.example.labmanagement.registration.service.RegistrationService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** MySQL acceptance for API-17..21, FR-10..14 and TC-REG-04..25. */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "LAB_TEST_DB_PASSWORD", matches = ".+")
@Transactional
class RegistrationMySqlIntegrationTest {

	private static final String INSTRUCTOR_EMAIL = "gv001@lab.local";
	private static final String STUDENT_EMAIL = "sv001@lab.local";

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void teachingRegistrationPersistsWholePendingAggregateWithoutAllocatingDevices() {
		RegistrationResponse created = registrationService.create(INSTRUCTOR_EMAIL,
				teachingRequest(List.of("TB0001", "TB0003"), null));

		assertThat(created.status()).isEqualTo(PhieuDangKyTrangThai.CHO_DUYET);
		assertThat(created.courseCode()).isEqualTo("INT5001");
		assertThat(created.schedules()).hasSize(2);
		assertThat(created.devices()).hasSize(2).allSatisfy(device -> assertThat(device.allocated()).isFalse());
		assertThat(count("PhieuDangKy", created.id())).isEqualTo(1);
		assertThat(count("PhieuGiangDay", created.id())).isEqualTo(1);
		assertThat(count("LichDangKy", created.id())).isEqualTo(2);
		assertThat(count("PhieuDangKyThietBi", created.id())).isEqualTo(2);
		assertThat(count("PhieuHuongDan", created.id())).isZero();
	}

	@Test
	void controlledStudentDeviceRequiresActiveSupervisorAndFailureLeavesNoOrphans() {
		long registrationsBefore = total("PhieuDangKy");
		RegistrationFormRequest missingSupervisor = learningRequest(List.of("TB0004"), null, null);

		assertThatThrownBy(() -> registrationService.create(STUDENT_EMAIL, missingSupervisor))
				.isInstanceOf(ApiException.class).hasMessageContaining("giảng viên hướng dẫn");
		assertThat(total("PhieuDangKy")).isEqualTo(registrationsBefore);

		RegistrationResponse created = registrationService.create(STUDENT_EMAIL,
				learningRequest(List.of("TB0004"), "GV001", null));
		assertThat(created.supervisorId()).isEqualTo("GV001");
		assertThat(count("PhieuHuongDan", created.id())).isEqualTo(1);
		assertThat(count("LichDangKy", created.id())).isEqualTo(2);
		assertThat(count("PhieuDangKyThietBi", created.id())).isEqualTo(1);

		assertThat(registrationService.get("gv001@lab.local", created.id()).id()).isEqualTo(created.id());
		assertThatThrownBy(() -> registrationService.get("gv002@lab.local", created.id()))
				.isInstanceOf(ApiException.class).hasMessageContaining("quyền xem");
		assertThat(registrationService.get("cb001@lab.local", created.id()).id()).isEqualTo(created.id());
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void updateReplacesChildrenAndForcesAggregateVersionIncrement() {
		String registrationId = null;
		try {
			RegistrationResponse created = registrationService.create(INSTRUCTOR_EMAIL,
					teachingRequest(List.of("TB0001", "TB0003"), null));
			registrationId = created.id();
			RegistrationFormRequest update = learningRequest(List.of("TB0003"), null, created.version());

			RegistrationResponse updated = registrationService.update(INSTRUCTOR_EMAIL, registrationId, update);

			assertThat(updated.type()).isEqualTo(LoaiPhieu.HOC_TAP);
			assertThat(updated.version()).isGreaterThan(created.version());
			assertThat(jdbcTemplate.queryForObject("SELECT VersionNo FROM PhieuDangKy WHERE MaPhieu = ?", Long.class,
					registrationId)).isEqualTo(updated.version());
			assertThat(count("PhieuGiangDay", registrationId)).isZero();
			assertThat(count("LichDangKy", registrationId)).isEqualTo(2);
			assertThat(count("PhieuDangKyThietBi", registrationId)).isEqualTo(1);
			assertThatThrownBy(() -> registrationService.update(INSTRUCTOR_EMAIL, created.id(), update))
					.isInstanceOf(ApiException.class).hasMessageContaining("yêu cầu khác");
			assertThat(count("LichDangKy", registrationId)).isEqualTo(2);
			assertThat(count("PhieuDangKyThietBi", registrationId)).isEqualTo(1);
		} finally {
			if (registrationId != null) {
				deleteRegistration(registrationId);
			}
		}
	}

	@Test
	void ownerCanCancelFutureRegistrationExactlyOnceButNotAfterAStartedSession() {
		RegistrationResponse cancellable = registrationService.create(INSTRUCTOR_EMAIL,
				teachingRequest(List.of("TB0001"), null));
		RegistrationResponse cancelled = registrationService.cancel(INSTRUCTOR_EMAIL, cancellable.id(),
				new RegistrationCancelRequest("Không còn nhu cầu", cancellable.version()));
		assertThat(cancelled.status()).isEqualTo(PhieuDangKyTrangThai.DA_HUY);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM XuLyPhieu WHERE MaPhieu = ? AND HanhDong = 'HUY'",
				Integer.class, cancellable.id())).isEqualTo(1);
		assertThatThrownBy(() -> registrationService.cancel(INSTRUCTOR_EMAIL, cancellable.id(),
				new RegistrationCancelRequest("Gửi lại", cancelled.version()))).isInstanceOf(ApiException.class);

		RegistrationResponse started = registrationService.create(INSTRUCTOR_EMAIL,
				teachingRequest(List.of("TB0001"), null));
		Long scheduleId = jdbcTemplate.queryForObject("SELECT MIN(MaLich) FROM LichDangKy WHERE MaPhieu = ?",
				Long.class, started.id());
		jdbcTemplate.update("INSERT INTO PhienSuDung (MaLich, NgaySuDung, TrangThai) VALUES (?, ?, 'VANG_MAT')",
				scheduleId, LocalDate.of(2027, 9, 6));

		assertThatThrownBy(() -> registrationService.cancel(INSTRUCTOR_EMAIL, started.id(),
				new RegistrationCancelRequest("Hủy sau phiên", started.version()))).isInstanceOf(ApiException.class)
				.hasMessageContaining("phiên sử dụng đầu tiên");
		assertThat(registrationService.get(INSTRUCTOR_EMAIL, started.id()).status())
				.isEqualTo(PhieuDangKyTrangThai.CHO_DUYET);
	}

	@Test
	void listScopeKeepsStudentInstructorAndManagerViewsSeparate() {
		RegistrationResponse supervised = registrationService.create(STUDENT_EMAIL,
				learningRequest(List.of("TB0004"), "GV001", null));
		RegistrationResponse instructorOwned = registrationService.create(INSTRUCTOR_EMAIL,
				teachingRequest(List.of("TB0001"), null));

		assertThat(registrationService.search(STUDENT_EMAIL, null, null, 0, 100).getContent())
				.extracting(item -> item.id()).contains(supervised.id()).doesNotContain(instructorOwned.id());
		assertThat(registrationService.search(INSTRUCTOR_EMAIL, null, null, 0, 100).getContent())
				.extracting(item -> item.id()).contains(supervised.id(), instructorOwned.id());
		assertThat(
				registrationService.search("cb001@lab.local", null, PhieuDangKyTrangThai.DA_DUYET, 0, 100).getContent())
				.allSatisfy(item -> assertThat(item.status()).isEqualTo(PhieuDangKyTrangThai.CHO_DUYET));
	}

	private RegistrationFormRequest teachingRequest(List<String> deviceIds, Long version) {
		return new RegistrationFormRequest(LoaiPhieu.GIANG_DAY, "Thực hành mạng Giai đoạn 5", "P0601", 20,
				LocalDate.of(2027, 9, 6), LocalDate.of(2027, 9, 12),
				List.of(new RegistrationScheduleRequest(2, 1), new RegistrationScheduleRequest(4, 2)), deviceIds,
				"INT5001", "N01", null, version);
	}

	private RegistrationFormRequest learningRequest(List<String> deviceIds, String supervisorId, Long version) {
		return new RegistrationFormRequest(LoaiPhieu.HOC_TAP, "Học tập Giai đoạn 5", "P0601", 10,
				LocalDate.of(2027, 9, 6), LocalDate.of(2027, 9, 12),
				List.of(new RegistrationScheduleRequest(2, 1), new RegistrationScheduleRequest(4, 2)), deviceIds, null,
				null, supervisorId, version);
	}

	private int count(String table, String registrationId) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE MaPhieu = ?", Integer.class,
				registrationId);
	}

	private long total(String table) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
	}

	private void deleteRegistration(String registrationId) {
		jdbcTemplate.update("DELETE FROM XuLyPhieu WHERE MaPhieu = ?", registrationId);
		jdbcTemplate.update("DELETE FROM PhieuHuongDan WHERE MaPhieu = ?", registrationId);
		jdbcTemplate.update("DELETE FROM PhieuGiangDay WHERE MaPhieu = ?", registrationId);
		jdbcTemplate.update("DELETE FROM PhieuDangKyThietBi WHERE MaPhieu = ?", registrationId);
		jdbcTemplate.update("DELETE FROM LichDangKy WHERE MaPhieu = ?", registrationId);
		jdbcTemplate.update("DELETE FROM PhieuDangKy WHERE MaPhieu = ?", registrationId);
	}
}
