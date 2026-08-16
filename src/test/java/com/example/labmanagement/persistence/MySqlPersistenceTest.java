package com.example.labmanagement.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.labmanagement.catalog.domain.LoaiThietBi;
import com.example.labmanagement.catalog.domain.NhomPhong;
import com.example.labmanagement.catalog.domain.Phong;
import com.example.labmanagement.catalog.domain.PhongTrangThai;
import com.example.labmanagement.catalog.domain.TaiNguyen;
import com.example.labmanagement.catalog.domain.ThietBi;
import com.example.labmanagement.catalog.domain.ThietBiTrangThai;
import com.example.labmanagement.catalog.repository.LoaiThietBiRepository;
import com.example.labmanagement.catalog.repository.NhomPhongRepository;
import com.example.labmanagement.catalog.repository.PhongRepository;
import com.example.labmanagement.catalog.repository.TaiNguyenRepository;
import com.example.labmanagement.catalog.repository.ThietBiRepository;
import com.example.labmanagement.identity.domain.NguoiDung;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.domain.VaiTro;
import com.example.labmanagement.identity.repository.NguoiDungRepository;
import com.example.labmanagement.identity.repository.VaiTroRepository;
import com.example.labmanagement.incident.domain.MucDoSuCo;
import com.example.labmanagement.incident.domain.SuCo;
import com.example.labmanagement.incident.domain.SuCoTrangThai;
import com.example.labmanagement.incident.repository.SuCoRepository;
import com.example.labmanagement.maintenance.domain.BaoTri;
import com.example.labmanagement.maintenance.domain.BaoTriSuCo;
import com.example.labmanagement.maintenance.domain.BaoTriTrangThai;
import com.example.labmanagement.maintenance.domain.TienDoBaoTri;
import com.example.labmanagement.maintenance.repository.BaoTriRepository;
import com.example.labmanagement.registration.domain.HanhDongXuLyPhieu;
import com.example.labmanagement.registration.domain.LichDangKy;
import com.example.labmanagement.registration.domain.LoaiPhieu;
import com.example.labmanagement.registration.domain.PhieuDangKy;
import com.example.labmanagement.registration.domain.PhieuDangKyThietBi;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import com.example.labmanagement.registration.domain.PhieuGiangDay;
import com.example.labmanagement.registration.domain.PhieuHuongDan;
import com.example.labmanagement.registration.domain.XuLyPhieu;
import com.example.labmanagement.registration.repository.PhieuDangKyRepository;
import com.example.labmanagement.scheduling.domain.LichChan;
import com.example.labmanagement.scheduling.domain.LichChanTrangThai;
import com.example.labmanagement.scheduling.domain.TietHoc;
import com.example.labmanagement.scheduling.repository.LichChanRepository;
import com.example.labmanagement.scheduling.repository.TietHocRepository;
import com.example.labmanagement.usage.domain.PhienSuDung;
import com.example.labmanagement.usage.domain.PhienSuDungThietBi;
import com.example.labmanagement.usage.domain.PhienSuDungTrangThai;
import com.example.labmanagement.usage.repository.PhienSuDungRepository;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "LAB_TEST_DB_PASSWORD", matches = ".+")
@Transactional
class MySqlPersistenceTest {

	private static final Class<?>[] BUSINESS_ENTITIES = {VaiTro.class, NguoiDung.class, NhomPhong.class, Phong.class,
			LoaiThietBi.class, ThietBi.class, TaiNguyen.class, TietHoc.class, PhieuDangKy.class, PhieuGiangDay.class,
			PhieuHuongDan.class, LichDangKy.class, PhieuDangKyThietBi.class, XuLyPhieu.class, LichChan.class,
			PhienSuDung.class, PhienSuDungThietBi.class, SuCo.class, BaoTri.class, BaoTriSuCo.class,
			TienDoBaoTri.class};

	@Autowired
	private Flyway flyway;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private jakarta.persistence.EntityManager entityManager;

	@Autowired
	private VaiTroRepository roleRepository;

	@Autowired
	private NguoiDungRepository userRepository;

	@Autowired
	private NhomPhongRepository roomGroupRepository;

	@Autowired
	private PhongRepository roomRepository;

	@Autowired
	private LoaiThietBiRepository deviceTypeRepository;

	@Autowired
	private ThietBiRepository deviceRepository;

	@Autowired
	private TaiNguyenRepository resourceRepository;

	@Autowired
	private TietHocRepository periodRepository;

	@Autowired
	private PhieuDangKyRepository registrationRepository;

	@Autowired
	private LichChanRepository blockedScheduleRepository;

	@Autowired
	private PhienSuDungRepository sessionRepository;

	@Autowired
	private SuCoRepository incidentRepository;

	@Autowired
	private BaoTriRepository maintenanceRepository;

	@Test
	void flywayIsValidAndSecondMigrationRunDoesNotRepeat() {
		flyway.validate();

		assertThat(flyway.info().applied()).hasSize(7);
		assertThat(flyway.migrate().migrationsExecuted).isZero();
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1",
				Integer.class)).isEqualTo(7);
	}

	@Test
	void seededIncidentsDoNotHaveFutureReportedTimes() {
		assertThat(jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM SuCo
				WHERE MaSuCo REGEXP '^SC(P)?[0-9]{4}$'
				  AND ThoiDiemBao > UTC_TIMESTAMP(6)
				""", Integer.class)).isZero();
	}

	@Test
	void schemaUsesInnoDbAndUtf8mb4() {
		List<Map<String, Object>> tables = jdbcTemplate.queryForList("""
				SELECT TABLE_NAME, ENGINE, TABLE_COLLATION
				FROM information_schema.TABLES
				WHERE TABLE_SCHEMA = DATABASE()
				  AND TABLE_NAME <> 'flyway_schema_history'
				""");

		assertThat(tables).hasSize(21).allSatisfy(table -> {
			assertThat(table.get("ENGINE")).isEqualTo("InnoDB");
			assertThat(table.get("TABLE_COLLATION").toString()).startsWith("utf8mb4_");
		});
	}

	@Test
	void seededDataCanBeQueriedThroughEveryEntityMapping() {
		assertThat(BUSINESS_ENTITIES).hasSize(21);
		Arrays.stream(BUSINESS_ENTITIES).forEach(entityType -> {
			Long count = entityManager
					.createQuery("select count(entity) from " + entityType.getSimpleName() + " entity", Long.class)
					.getSingleResult();
			assertThat(count).as(entityType.getSimpleName()).isPositive();
		});
	}

	@Test
	void savesAndQueriesEveryPersistenceGroup() {
		VaiTro role = roleRepository.save(new VaiTro("S1", "Vai trò kiểm thử Giai đoạn 1"));
		NguoiDung user = userRepository.save(new NguoiDung("S1USER", "Người kiểm thử", "s1-user@lab.local",
				"not-a-real-password", "Kiểm thử", role, NguoiDungTrangThai.HOAT_DONG));
		NhomPhong roomGroup = roomGroupRepository.save(new NhomPhong("S1GROUP", "Nhóm phòng Giai đoạn 1", null));
		Phong room = roomRepository.save(
				new Phong("S1ROOM", "Phòng Giai đoạn 1", roomGroup, "Tầng kiểm thử", 20, PhongTrangThai.SAN_SANG));
		LoaiThietBi deviceType = deviceTypeRepository
				.save(new LoaiThietBi("S1TYPE", "Thiết bị Giai đoạn 1", false, true, null));
		ThietBi device = deviceRepository.save(new ThietBi("S1DEVICE", "Thiết bị Giai đoạn 1", deviceType, "S1-SERIAL",
				"S1-MODEL", room, ThietBiTrangThai.SAN_SANG));
		TaiNguyen roomResource = resourceRepository.save(TaiNguyen.forRoom("S1-ROOM-RESOURCE", room));
		TaiNguyen deviceResource = resourceRepository.save(TaiNguyen.forDevice("S1-DEVICE-RESOURCE", device));
		TietHoc period = periodRepository
				.save(new TietHoc(99, "Tiết kiểm thử", LocalTime.of(20, 0), LocalTime.of(21, 0)));
		PhieuDangKy registration = registrationRepository
				.save(new PhieuDangKy("S1REG", user, room, LoaiPhieu.GIANG_DAY, "Kiểm thử persistence", 10,
						LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 1), PhieuDangKyTrangThai.DA_DUYET));
		entityManager.persist(new PhieuGiangDay(registration, "S1COURSE", "S1CLASS"));
		entityManager.persist(new PhieuHuongDan(registration, user));
		LichDangKy schedule = new LichDangKy(registration, 8, period);
		entityManager.persist(schedule);
		entityManager.persist(new PhieuDangKyThietBi(registration, device, true));
		entityManager.persist(new XuLyPhieu(registration, user, HanhDongXuLyPhieu.PHE_DUYET, null,
				Instant.parse("2027-01-01T00:00:00Z")));
		LichChan blockedSchedule = blockedScheduleRepository.save(new LichChan(roomResource, LocalDate.of(2027, 2, 1),
				LocalDate.of(2027, 2, 2), null, null, "Kiểm thử lịch chặn", LichChanTrangThai.HIEU_LUC, user));
		entityManager.flush();

		PhienSuDung session = sessionRepository
				.save(new PhienSuDung(schedule, LocalDate.of(2027, 1, 1), PhienSuDungTrangThai.HOAN_THANH,
						Instant.parse("2027-01-01T01:00:00Z"), Instant.parse("2027-01-01T02:00:00Z"), user, user));
		entityManager.flush();
		entityManager.persist(new PhienSuDungThietBi(session, device, "Tốt", "Tốt", null));
		SuCo incident = incidentRepository
				.save(new SuCo("S1INCIDENT", deviceResource, session, user, user, MucDoSuCo.TRUNG_BINH,
						"Kiểm thử sự cố", SuCoTrangThai.DANG_XU_LY, Instant.parse("2027-01-01T01:30:00Z"), null, null));
		BaoTri maintenance = maintenanceRepository.save(new BaoTri("S1MAINTENANCE", deviceResource, user,
				Instant.parse("2027-01-02T01:00:00Z"), null, "Kiểm thử bảo trì", BaoTriTrangThai.DANG_BAO_TRI, null));
		entityManager.persist(new BaoTriSuCo(maintenance, incident));
		entityManager.persist(new TienDoBaoTri(maintenance, Instant.parse("2027-01-02T01:00:00Z"),
				BaoTriTrangThai.DANG_BAO_TRI, "Đang kiểm thử", user));
		entityManager.flush();
		entityManager.clear();

		assertThat(userRepository.findByEmailIgnoreCase("s1-user@lab.local")).isPresent();
		assertThat(deviceRepository.findBySerialNumber("S1-SERIAL")).isPresent();
		assertThat(registrationRepository.findById("S1REG")).isPresent();
		assertThat(blockedScheduleRepository.findById(blockedSchedule.getId())).isPresent();
		assertThat(sessionRepository.findById(session.getId())).isPresent();
		assertThat(incidentRepository.findById("S1INCIDENT")).isPresent();
		assertThat(maintenanceRepository.findById("S1MAINTENANCE")).isPresent();
	}

	@Test
	void uniqueEmailSerialAndResourceIdAreEnforced() {
		assertSqlConstraintViolation("""
				INSERT INTO NguoiDung
				  (MaNguoiDung, HoTen, Email, MatKhau, MaVaiTro, TrangThai)
				VALUES ('S1-DUP-EMAIL', 'Trùng email', 'cb001@lab.local', 'x', 'CBQL', 'HOAT_DONG')
				""", 1062);
		assertSqlConstraintViolation("""
				INSERT INTO ThietBi
				  (MaThietBi, TenThietBi, MaLoai, SoSerial, TrangThai)
				VALUES ('S1-DUP-SERIAL', 'Trùng serial', 'PC', 'SERIAL-000001', 'SAN_SANG')
				""", 1062);
		assertSqlConstraintViolation("""
				INSERT INTO TaiNguyen (MaTaiNguyen, LoaiTaiNguyen, MaPhong)
				VALUES ('TN-P0601', 'PHONG', 'P0602')
				""", 1062);
	}

	@Test
	void foreignKeyAndCheckConstraintsAreEnforced() {
		assertSqlConstraintViolation("""
				INSERT INTO NguoiDung
				  (MaNguoiDung, HoTen, Email, MatKhau, MaVaiTro, TrangThai)
				VALUES ('S1-BAD-FK', 'Sai khóa ngoại', 'bad-fk@lab.local', 'x', 'MISSING', 'HOAT_DONG')
				""", 1452);
		assertSqlConstraintViolation("""
				INSERT INTO Phong
				  (MaPhong, TenPhong, MaNhom, ViTri, SucChua, TrangThai)
				VALUES ('S1-BAD-CHECK', 'Sai sức chứa', 'NP01', 'Kiểm thử', 0, 'SAN_SANG')
				""", 3819);
	}

	@Test
	void generatedColumnUniqueIndexesPreventDuplicateResources() {
		assertSqlConstraintViolation("""
				INSERT INTO TaiNguyen (MaTaiNguyen, LoaiTaiNguyen, MaPhong)
				VALUES ('S1-DUP-ROOM-RESOURCE', 'PHONG', 'P0601')
				""", 1062);
		assertSqlConstraintViolation("""
				INSERT INTO TaiNguyen (MaTaiNguyen, LoaiTaiNguyen, MaThietBi)
				VALUES ('S1-DUP-DEVICE-RESOURCE', 'THIET_BI', 'TB0001')
				""", 1062);
	}

	@Test
	void registrationDecisionHistoryQueryUsesCompositeIndex() {
		List<String> indexes = jdbcTemplate.query("EXPLAIN SELECT * FROM XuLyPhieu WHERE MaPhieu = ? ORDER BY ThoiDiem",
				(resultSet, rowNumber) -> resultSet.getString("key"), "PDK0001");

		assertThat(indexes).contains("IX_XuLy_PhieuThoiDiem");
	}

	private void assertSqlConstraintViolation(String sql, int errorCode) {
		assertThatThrownBy(() -> jdbcTemplate.update(sql)).isInstanceOf(DataAccessException.class)
				.satisfies(exception -> {
					Throwable rootCause = exception;
					while (rootCause.getCause() != null) {
						rootCause = rootCause.getCause();
					}
					assertThat(rootCause).isInstanceOf(SQLException.class);
					assertThat(((SQLException) rootCause).getErrorCode()).isEqualTo(errorCode);
				});
	}
}
