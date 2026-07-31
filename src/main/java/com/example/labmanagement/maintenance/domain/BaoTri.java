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
}
