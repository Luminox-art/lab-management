package com.example.labmanagement.registration.domain;

import com.example.labmanagement.catalog.domain.Phong;
import com.example.labmanagement.common.metadata.AuditedVersionedEntity;
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
import java.time.LocalDate;

@Entity
@Table(name = "PhieuDangKy")
public class PhieuDangKy extends AuditedVersionedEntity {

	@Id
	@Column(name = "MaPhieu", nullable = false, length = 50)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "MaNguoiTao", nullable = false)
	private NguoiDung creator;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "MaPhong", nullable = false)
	private Phong room;

	@Enumerated(EnumType.STRING)
	@Column(name = "LoaiPhieu", nullable = false, length = 20)
	private LoaiPhieu type;

	@Column(name = "MucDich", nullable = false, columnDefinition = "TEXT")
	private String purpose;

	@Column(name = "SoNguoi", nullable = false)
	private int attendeeCount;

	@Column(name = "NgayBatDau", nullable = false)
	private LocalDate startDate;

	@Column(name = "NgayKetThuc", nullable = false)
	private LocalDate endDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "TrangThai", nullable = false, length = 20)
	private PhieuDangKyTrangThai status;

	protected PhieuDangKy() {
	}

	public PhieuDangKy(String id, NguoiDung creator, Phong room, LoaiPhieu type, String purpose, int attendeeCount,
			LocalDate startDate, LocalDate endDate, PhieuDangKyTrangThai status) {
		this.id = id;
		this.creator = creator;
		this.room = room;
		this.type = type;
		this.purpose = purpose;
		this.attendeeCount = attendeeCount;
		this.startDate = startDate;
		this.endDate = endDate;
		this.status = status;
	}

	public String getId() {
		return id;
	}

	public LoaiPhieu getType() {
		return type;
	}

	public Phong getRoom() {
		return room;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public PhieuDangKyTrangThai getStatus() {
		return status;
	}
}
