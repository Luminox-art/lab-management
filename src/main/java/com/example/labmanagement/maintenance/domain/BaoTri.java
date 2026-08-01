package com.example.labmanagement.maintenance.domain;

import com.example.labmanagement.catalog.domain.TaiNguyen;
import com.example.labmanagement.common.metadata.VersionedEntity;
import com.example.labmanagement.identity.domain.NguoiDung;
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
@Table(name = "BaoTri")
public class BaoTri extends VersionedEntity {

	@Id
	@Column(name = "MaBaoTri", nullable = false, length = 50)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "MaTaiNguyen", nullable = false)
	private TaiNguyen resource;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "MaNguoiPhuTrach", nullable = false)
	private NguoiDung assignee;

	@Column(name = "NgayBatDau", nullable = false)
	private Instant startAt;

	@Column(name = "NgayKetThuc")
	private Instant endAt;

	@Column(name = "NoiDung", nullable = false, columnDefinition = "TEXT")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(name = "TrangThai", nullable = false, length = 20)
	private BaoTriTrangThai status;

	@Column(name = "KetQua", columnDefinition = "TEXT")
	private String result;

	protected BaoTri() {
	}

	public BaoTri(String id, TaiNguyen resource, NguoiDung assignee, Instant startAt, Instant endAt, String content,
			BaoTriTrangThai status, String result) {
		this.id = id;
		this.resource = resource;
		this.assignee = assignee;
		this.startAt = startAt;
		this.endAt = endAt;
		this.content = content;
		this.status = status;
		this.result = result;
	}

	public String getId() {
		return id;
	}

	public BaoTriTrangThai getStatus() {
		return status;
	}

	public TaiNguyen getResource() {
		return resource;
	}

	public NguoiDung getAssignee() {
		return assignee;
	}

	public Instant getStartAt() {
		return startAt;
	}

	public Instant getEndAt() {
		return endAt;
	}

	public String getContent() {
		return content;
	}

	public String getResult() {
		return result;
	}

	public void updateProgress(BaoTriTrangThai nextStatus, Instant requestedEndAt, String requestedResult) {
		if (status == BaoTriTrangThai.HOAN_THANH || status == BaoTriTrangThai.DA_HUY) {
			throw new IllegalStateException("Bảo trì đã kết thúc và không thể cập nhật.");
		}
		boolean allowed = status == BaoTriTrangThai.CHO_XU_LY
				? nextStatus == BaoTriTrangThai.CHO_XU_LY || nextStatus == BaoTriTrangThai.DANG_BAO_TRI
						|| nextStatus == BaoTriTrangThai.DA_HUY
				: nextStatus == BaoTriTrangThai.DANG_BAO_TRI || nextStatus == BaoTriTrangThai.HOAN_THANH
						|| nextStatus == BaoTriTrangThai.DA_HUY;
		if (!allowed) {
			throw new IllegalStateException("Chuyển trạng thái bảo trì không hợp lệ.");
		}
		if (nextStatus == BaoTriTrangThai.HOAN_THANH) {
			if (requestedEndAt == null) {
				throw new IllegalStateException("Hoàn thành bảo trì bắt buộc phải có ngày kết thúc.");
			}
			if (requestedEndAt.isBefore(startAt)) {
				throw new IllegalStateException("Ngày kết thúc không được trước ngày bắt đầu bảo trì.");
			}
			if (requestedResult == null || requestedResult.isBlank()) {
				throw new IllegalStateException("Hoàn thành bảo trì bắt buộc phải có kết quả.");
			}
		}
		this.status = nextStatus;
		this.endAt = nextStatus == BaoTriTrangThai.HOAN_THANH ? requestedEndAt : null;
		this.result = requestedResult;
	}
}
