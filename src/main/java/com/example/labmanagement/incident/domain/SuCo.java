package com.example.labmanagement.incident.domain;

import com.example.labmanagement.catalog.domain.TaiNguyen;
import com.example.labmanagement.common.metadata.VersionedEntity;
import com.example.labmanagement.identity.domain.NguoiDung;
import com.example.labmanagement.usage.domain.PhienSuDung;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "SuCo")
public class SuCo extends VersionedEntity {

	@Id
	@Column(name = "MaSuCo", nullable = false, length = 50)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "MaTaiNguyen", nullable = false)
	private TaiNguyen resource;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaPhien")
	private PhienSuDung session;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "MaNguoiBao", nullable = false)
	private NguoiDung reporter;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaNguoiXuLy")
	private NguoiDung handler;

	@Enumerated(EnumType.STRING)
	@Column(name = "MucDo", nullable = false, length = 20)
	private MucDoSuCo severity;

	@Column(name = "MoTa", nullable = false, columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "TrangThai", nullable = false, length = 20)
	private SuCoTrangThai status;

	@Column(name = "ThoiDiemBao", nullable = false)
	private Instant reportedAt;

	@Column(name = "ThoiDiemHoanThanh")
	private Instant completedAt;

	@Column(name = "KetQua", columnDefinition = "TEXT")
	private String result;

	protected SuCo() {
	}

	public SuCo(String id, TaiNguyen resource, PhienSuDung session, NguoiDung reporter, NguoiDung handler,
			MucDoSuCo severity, String description, SuCoTrangThai status, Instant reportedAt, Instant completedAt,
			String result) {
		this.id = id;
		this.resource = resource;
		this.session = session;
		this.reporter = reporter;
		this.handler = handler;
		this.severity = severity;
		this.description = description;
		this.status = status;
		this.reportedAt = reportedAt;
		this.completedAt = completedAt;
		this.result = result;
	}

	public String getId() {
		return id;
	}

	public MucDoSuCo getSeverity() {
		return severity;
	}

	public TaiNguyen getResource() {
		return resource;
	}

	public PhienSuDung getSession() {
		return session;
	}

	public NguoiDung getReporter() {
		return reporter;
	}

	public NguoiDung getHandler() {
		return handler;
	}

	public String getDescription() {
		return description;
	}

	public SuCoTrangThai getStatus() {
		return status;
	}

	public Instant getReportedAt() {
		return reportedAt;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}

	public String getResult() {
		return result;
	}

	public void updateHandling(NguoiDung assignedHandler, SuCoTrangThai nextStatus, String handlingResult,
			Instant now) {
		if (status == SuCoTrangThai.DA_XU_LY || status == SuCoTrangThai.DA_HUY) {
			throw new IllegalStateException("Sự cố đã kết thúc và không thể cập nhật.");
		}
		boolean allowed = status == SuCoTrangThai.MOI
				? nextStatus == SuCoTrangThai.DANG_XU_LY || nextStatus == SuCoTrangThai.DA_HUY
				: nextStatus == SuCoTrangThai.DANG_XU_LY || nextStatus == SuCoTrangThai.DA_XU_LY
						|| nextStatus == SuCoTrangThai.DA_HUY;
		if (!allowed) {
			throw new IllegalStateException("Chuyển trạng thái sự cố không hợp lệ.");
		}
		if ((nextStatus == SuCoTrangThai.DANG_XU_LY || nextStatus == SuCoTrangThai.DA_XU_LY)
				&& assignedHandler == null) {
			throw new IllegalStateException("Sự cố đang xử lý hoặc đã xử lý phải có người xử lý.");
		}
		if (nextStatus == SuCoTrangThai.DA_XU_LY && (handlingResult == null || handlingResult.isBlank())) {
			throw new IllegalStateException("Hoàn thành sự cố bắt buộc phải có kết quả xử lý.");
		}
		this.handler = assignedHandler;
		this.status = nextStatus;
		this.result = handlingResult;
		this.completedAt = nextStatus == SuCoTrangThai.DA_XU_LY ? now : null;
	}
}
