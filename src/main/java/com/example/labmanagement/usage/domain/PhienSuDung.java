package com.example.labmanagement.usage.domain;

import com.example.labmanagement.common.metadata.VersionedEntity;
import com.example.labmanagement.identity.domain.NguoiDung;
import com.example.labmanagement.registration.domain.LichDangKy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "PhienSuDung")
public class PhienSuDung extends VersionedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaPhien", nullable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "MaLich", nullable = false)
	private LichDangKy schedule;

	@Column(name = "NgaySuDung", nullable = false)
	private LocalDate usageDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "TrangThai", nullable = false, length = 20)
	private PhienSuDungTrangThai status;

	@Column(name = "ThoiDiemCheckIn")
	private Instant checkedInAt;

	@Column(name = "ThoiDiemCheckOut")
	private Instant checkedOutAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaNguoiCheckIn")
	private NguoiDung checkedInBy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaNguoiCheckOut")
	private NguoiDung checkedOutBy;

	protected PhienSuDung() {
	}

	public PhienSuDung(LichDangKy schedule, LocalDate usageDate, PhienSuDungTrangThai status, Instant checkedInAt,
			Instant checkedOutAt, NguoiDung checkedInBy, NguoiDung checkedOutBy) {
		this.schedule = schedule;
		this.usageDate = usageDate;
		this.status = status;
		this.checkedInAt = checkedInAt;
		this.checkedOutAt = checkedOutAt;
		this.checkedInBy = checkedInBy;
		this.checkedOutBy = checkedOutBy;
	}

	public Long getId() {
		return id;
	}

	public PhienSuDungTrangThai getStatus() {
		return status;
	}

	public LichDangKy getSchedule() {
		return schedule;
	}

	public LocalDate getUsageDate() {
		return usageDate;
	}

	public Instant getCheckedInAt() {
		return checkedInAt;
	}

	public Instant getCheckedOutAt() {
		return checkedOutAt;
	}

	public NguoiDung getCheckedInBy() {
		return checkedInBy;
	}

	public NguoiDung getCheckedOutBy() {
		return checkedOutBy;
	}

	public void checkIn(NguoiDung actor, Instant checkedInAt) {
		this.status = PhienSuDungTrangThai.DANG_SU_DUNG;
		this.checkedInBy = actor;
		this.checkedInAt = checkedInAt;
	}

	public void checkOut(NguoiDung actor, Instant checkedOutAt) {
		this.status = PhienSuDungTrangThai.HOAN_THANH;
		this.checkedOutBy = actor;
		this.checkedOutAt = checkedOutAt;
	}

	public void markAbsent() {
		this.status = PhienSuDungTrangThai.VANG_MAT;
	}
}
