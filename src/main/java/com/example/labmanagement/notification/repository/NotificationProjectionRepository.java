package com.example.labmanagement.notification.repository;

import com.example.labmanagement.common.clock.TimeConfiguration;
import com.example.labmanagement.notification.domain.NotificationType;
import com.example.labmanagement.notification.dto.NotificationResponse;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationProjectionRepository {

	private final JdbcTemplate jdbcTemplate;

	public NotificationProjectionRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<NotificationResponse> findAccessible(String userId, LocalDate upcomingFrom, LocalDate upcomingTo) {
		List<NotificationResponse> notifications = new ArrayList<>();
		notifications.addAll(registrationDecisions(userId));
		notifications.addAll(upcomingSessions(userId, upcomingFrom, upcomingTo));
		notifications.addAll(incidents(userId));
		notifications.addAll(maintenanceProgress(userId));
		return notifications;
	}

	private List<NotificationResponse> registrationDecisions(String userId) {
		return jdbcTemplate.query("""
				SELECT history.MaXuLy, history.HanhDong, history.LyDo, history.ThoiDiem,
				       registration.MaPhieu
				FROM xulyphieu history
				JOIN phieudangky registration ON registration.MaPhieu = history.MaPhieu
				WHERE registration.MaNguoiTao = ?
				   OR EXISTS (
				       SELECT 1 FROM phieuhuongdan supervision
				       WHERE supervision.MaPhieu = registration.MaPhieu
				         AND supervision.MaGVHuongDan = ?)
				""", (resultSet, rowNumber) -> {
			String action = resultSet.getString("HanhDong");
			String registrationId = resultSet.getString("MaPhieu");
			String title = switch (action) {
				case "PHE_DUYET" -> "Phiếu đăng ký đã được duyệt";
				case "TU_CHOI" -> "Phiếu đăng ký đã bị từ chối";
				default -> "Phiếu đăng ký đã được hủy";
			};
			String reason = resultSet.getString("LyDo");
			String content = reason == null || reason.isBlank()
					? "Phiếu " + registrationId + " vừa có quyết định mới."
					: "Phiếu " + registrationId + ": " + reason;
			return new NotificationResponse("REG-" + resultSet.getLong("MaXuLy"), NotificationType.PHIEU_DANG_KY, title,
					content, displayInstant(resultSet.getTimestamp("ThoiDiem")), "/registrations/" + registrationId,
					true);
		}, userId, userId);
	}

	private List<NotificationResponse> upcomingSessions(String userId, LocalDate from, LocalDate to) {
		return jdbcTemplate.query("""
				SELECT DISTINCT session.MaPhien, session.NgaySuDung, period.GioBatDau,
				       registration.MaPhieu, room.MaPhong, room.TenPhong
				FROM phiensudung session
				JOIN lichdangky schedule ON schedule.MaLich = session.MaLich
				JOIN tiethoc period ON period.MaTiet = schedule.MaTiet
				JOIN phieudangky registration ON registration.MaPhieu = schedule.MaPhieu
				JOIN phong room ON room.MaPhong = registration.MaPhong
				WHERE session.TrangThai = 'CHUA_BAT_DAU'
				  AND session.NgaySuDung BETWEEN ? AND ?
				  AND (registration.MaNguoiTao = ?
				       OR EXISTS (
				           SELECT 1 FROM phieuhuongdan supervision
				           WHERE supervision.MaPhieu = registration.MaPhieu
				             AND supervision.MaGVHuongDan = ?))
				""", (resultSet, rowNumber) -> {
			Long sessionId = resultSet.getLong("MaPhien");
			LocalDate usageDate = resultSet.getObject("NgaySuDung", LocalDate.class);
			LocalDateTime scheduledAt = LocalDateTime.of(usageDate,
					resultSet.getObject("GioBatDau", java.time.LocalTime.class));
			return new NotificationResponse("SESSION-" + sessionId, NotificationType.PHIEN_SAP_DIEN_RA,
					"Phiên sử dụng sắp diễn ra",
					"Phiên #" + sessionId + " tại " + resultSet.getString("MaPhong") + " — "
							+ resultSet.getString("TenPhong") + ".",
					scheduledAt.atZone(TimeConfiguration.DISPLAY_ZONE).toOffsetDateTime(), "/sessions/" + sessionId,
					true);
		}, from, to, userId, userId);
	}

	private List<NotificationResponse> incidents(String userId) {
		return jdbcTemplate.query("""
				SELECT DISTINCT incident.MaSuCo, incident.MucDo, incident.MoTa, incident.TrangThai,
				       incident.ThoiDiemBao
				FROM suco incident
				LEFT JOIN phiensudung session ON session.MaPhien = incident.MaPhien
				LEFT JOIN lichdangky schedule ON schedule.MaLich = session.MaLich
				LEFT JOIN phieudangky registration ON registration.MaPhieu = schedule.MaPhieu
				WHERE incident.MaNguoiBao = ? OR incident.MaNguoiXuLy = ?
				   OR registration.MaNguoiTao = ?
				   OR EXISTS (
				       SELECT 1 FROM phieuhuongdan supervision
				       WHERE supervision.MaPhieu = registration.MaPhieu
				         AND supervision.MaGVHuongDan = ?)
				""", (resultSet, rowNumber) -> {
			String incidentId = resultSet.getString("MaSuCo");
			return new NotificationResponse("INCIDENT-" + incidentId, NotificationType.SU_CO,
					"Sự cố liên quan — " + resultSet.getString("MucDo"),
					incidentId + " · " + resultSet.getString("TrangThai") + ": " + resultSet.getString("MoTa"),
					displayInstant(resultSet.getTimestamp("ThoiDiemBao")), "/incidents/" + incidentId, true);
		}, userId, userId, userId, userId);
	}

	private List<NotificationResponse> maintenanceProgress(String userId) {
		return jdbcTemplate.query("""
				SELECT DISTINCT progress.MaTienDo, progress.ThoiDiem, progress.TrangThai, progress.NoiDung,
				       maintenance.MaBaoTri, maintenance.MaNguoiPhuTrach, sourceIncident.MaSuCo
				FROM tiendobaotri progress
				JOIN baotri maintenance ON maintenance.MaBaoTri = progress.MaBaoTri
				LEFT JOIN baotrisuco sourceLink ON sourceLink.MaBaoTri = maintenance.MaBaoTri
				LEFT JOIN suco sourceIncident ON sourceIncident.MaSuCo = sourceLink.MaSuCo
				LEFT JOIN phiensudung session ON session.MaPhien = sourceIncident.MaPhien
				LEFT JOIN lichdangky schedule ON schedule.MaLich = session.MaLich
				LEFT JOIN phieudangky registration ON registration.MaPhieu = schedule.MaPhieu
				WHERE maintenance.MaNguoiPhuTrach = ? OR sourceIncident.MaNguoiBao = ?
				   OR registration.MaNguoiTao = ?
				   OR EXISTS (
				       SELECT 1 FROM phieuhuongdan supervision
				       WHERE supervision.MaPhieu = registration.MaPhieu
				         AND supervision.MaGVHuongDan = ?)
				""", (resultSet, rowNumber) -> {
			String maintenanceId = resultSet.getString("MaBaoTri");
			String sourceIncidentId = resultSet.getString("MaSuCo");
			String targetUrl = userId.equals(resultSet.getString("MaNguoiPhuTrach"))
					? "/maintenances/" + maintenanceId
					: sourceIncidentId == null ? "/notifications" : "/incidents/" + sourceIncidentId;
			return new NotificationResponse("MAINTENANCE-" + resultSet.getLong("MaTienDo"),
					NotificationType.TIEN_DO_BAO_TRI, "Tiến độ bảo trì " + maintenanceId,
					resultSet.getString("TrangThai") + ": " + resultSet.getString("NoiDung"),
					displayInstant(resultSet.getTimestamp("ThoiDiem")), targetUrl, true);
		}, userId, userId, userId, userId);
	}

	private OffsetDateTime displayInstant(Timestamp timestamp) {
		return timestamp.toInstant().atZone(TimeConfiguration.DISPLAY_ZONE).toOffsetDateTime();
	}
}
