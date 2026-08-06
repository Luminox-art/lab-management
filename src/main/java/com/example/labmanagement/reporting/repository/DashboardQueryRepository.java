package com.example.labmanagement.reporting.repository;

import com.example.labmanagement.catalog.domain.LoaiTaiNguyen;
import com.example.labmanagement.common.clock.TimeConfiguration;
import com.example.labmanagement.incident.domain.MucDoSuCo;
import com.example.labmanagement.maintenance.domain.BaoTriTrangThai;
import com.example.labmanagement.reporting.domain.DashboardGroup;
import com.example.labmanagement.reporting.dto.DashboardFrequencyResponse;
import com.example.labmanagement.reporting.dto.DashboardMaintenanceResponse;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardQueryRepository {

	private final JdbcTemplate jdbcTemplate;

	public DashboardQueryRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public DashboardCounts counts(LocalDate from, LocalDate to) {
		return jdbcTemplate.queryForObject("""
				SELECT
				  SUM(CASE WHEN session.TrangThai IN ('DANG_SU_DUNG','HOAN_THANH') THEN 1 ELSE 0 END) actual_sessions,
				  SUM(CASE WHEN session.TrangThai = 'HOAN_THANH' THEN 1 ELSE 0 END) completed_sessions,
				  SUM(CASE WHEN session.TrangThai = 'DANG_SU_DUNG' THEN 1 ELSE 0 END) in_progress_sessions,
				  SUM(CASE WHEN session.TrangThai = 'VANG_MAT' THEN 1 ELSE 0 END) absent_sessions,
				  (SELECT COUNT(*) FROM SuCo incident
				   WHERE DATE(incident.ThoiDiemBao) BETWEEN ? AND ?) incidents,
				  (SELECT COUNT(*) FROM BaoTri maintenance
				   WHERE maintenance.TrangThai IN ('CHO_XU_LY','DANG_BAO_TRI')
				     AND DATE(maintenance.NgayBatDau) <= ?) active_maintenances
				FROM PhienSuDung session
				WHERE session.NgaySuDung BETWEEN ? AND ?
				""",
				(resultSet, rowNumber) -> new DashboardCounts(resultSet.getLong("actual_sessions"),
						resultSet.getLong("completed_sessions"), resultSet.getLong("in_progress_sessions"),
						resultSet.getLong("absent_sessions"), resultSet.getLong("incidents"),
						resultSet.getLong("active_maintenances")),
				from, to, to, from, to);
	}

	public List<DashboardFrequencyResponse> frequencies(LocalDate from, LocalDate to, DashboardGroup group) {
		if (group == DashboardGroup.PHONG) {
			return jdbcTemplate.query(
					"""
							SELECT room.MaPhong id, room.TenPhong name, COUNT(*) frequency
							FROM PhienSuDung session
							JOIN LichDangKy schedule ON schedule.MaLich = session.MaLich
							JOIN PhieuDangKy registration ON registration.MaPhieu = schedule.MaPhieu
							JOIN Phong room ON room.MaPhong = registration.MaPhong
							WHERE session.NgaySuDung BETWEEN ? AND ?
							  AND session.TrangThai IN ('DANG_SU_DUNG','HOAN_THANH')
							GROUP BY room.MaPhong, room.TenPhong
							ORDER BY frequency DESC, room.MaPhong
							LIMIT 20
							""", (resultSet, rowNumber) -> new DashboardFrequencyResponse(group,
							resultSet.getString("id"), resultSet.getString("name"), resultSet.getLong("frequency"), 0),
					from, to);
		}
		return jdbcTemplate.query("""
				SELECT device.MaThietBi id, device.TenThietBi name, COUNT(*) frequency
				FROM PhienSuDung session
				JOIN PhienSuDungThietBi usedDevice ON usedDevice.MaPhien = session.MaPhien
				JOIN ThietBi device ON device.MaThietBi = usedDevice.MaThietBi
				WHERE session.NgaySuDung BETWEEN ? AND ?
				  AND session.TrangThai IN ('DANG_SU_DUNG','HOAN_THANH')
				GROUP BY device.MaThietBi, device.TenThietBi
				ORDER BY frequency DESC, device.MaThietBi
				LIMIT 20
				""", (resultSet, rowNumber) -> new DashboardFrequencyResponse(group, resultSet.getString("id"),
				resultSet.getString("name"), resultSet.getLong("frequency"), 0), from, to);
	}

	public Map<MucDoSuCo, Long> incidentSeverities(LocalDate from, LocalDate to) {
		return jdbcTemplate
				.query("""
						SELECT incident.MucDo severity, COUNT(*) incident_count
						FROM SuCo incident
						WHERE DATE(incident.ThoiDiemBao) BETWEEN ? AND ?
						GROUP BY incident.MucDo
						""",
						(resultSet, rowNumber) -> Map.entry(MucDoSuCo.valueOf(resultSet.getString("severity")),
								resultSet.getLong("incident_count")),
						from, to)
				.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	public List<DashboardMaintenanceResponse> activeMaintenances(LocalDate to) {
		return jdbcTemplate.query("""
				SELECT maintenance.MaBaoTri, maintenance.MaTaiNguyen, resource.LoaiTaiNguyen,
				       COALESCE(room.MaPhong, device.MaThietBi) reference_id,
				       COALESCE(room.TenPhong, device.TenThietBi) resource_name,
				       maintenance.TrangThai, maintenance.NgayBatDau
				FROM BaoTri maintenance
				JOIN TaiNguyen resource ON resource.MaTaiNguyen = maintenance.MaTaiNguyen
				LEFT JOIN Phong room ON room.MaPhong = resource.MaPhong
				LEFT JOIN ThietBi device ON device.MaThietBi = resource.MaThietBi
				WHERE maintenance.TrangThai IN ('CHO_XU_LY','DANG_BAO_TRI')
				  AND DATE(maintenance.NgayBatDau) <= ?
				ORDER BY maintenance.NgayBatDau DESC, maintenance.MaBaoTri
				LIMIT 20
				""",
				(resultSet, rowNumber) -> new DashboardMaintenanceResponse(resultSet.getString("MaBaoTri"),
						resultSet.getString("MaTaiNguyen"), LoaiTaiNguyen.valueOf(resultSet.getString("LoaiTaiNguyen")),
						resultSet.getString("reference_id"), resultSet.getString("resource_name"),
						BaoTriTrangThai.valueOf(resultSet.getString("TrangThai")),
						display(resultSet.getTimestamp("NgayBatDau"))),
				to);
	}

	private java.time.OffsetDateTime display(Timestamp timestamp) {
		return timestamp.toInstant().atZone(TimeConfiguration.DISPLAY_ZONE).toOffsetDateTime();
	}
}
