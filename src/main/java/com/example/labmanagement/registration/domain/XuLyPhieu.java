package com.example.labmanagement.registration.domain;

import com.example.labmanagement.identity.domain.NguoiDung;
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

@Entity
@Table(name = "XuLyPhieu")
public class XuLyPhieu {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaXuLy", nullable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "MaPhieu", nullable = false)
	private PhieuDangKy registration;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "MaNguoiXuLy", nullable = false)
	private NguoiDung handler;

	@Enumerated(EnumType.STRING)
	@Column(name = "HanhDong", nullable = false, length = 20)
	private HanhDongXuLyPhieu action;

	@Column(name = "LyDo", columnDefinition = "TEXT")
	private String reason;

	@Column(name = "ThoiDiem", nullable = false)
	private Instant occurredAt;

	protected XuLyPhieu() {
	}

	public XuLyPhieu(PhieuDangKy registration, NguoiDung handler, HanhDongXuLyPhieu action, String reason,
			Instant occurredAt) {
		this.registration = registration;
		this.handler = handler;
		this.action = action;
		this.reason = reason;
		this.occurredAt = occurredAt;
	}

	public Long getId() {
		return id;
	}

	public PhieuDangKy getRegistration() {
		return registration;
	}

	public NguoiDung getHandler() {
		return handler;
	}

	public HanhDongXuLyPhieu getAction() {
		return action;
	}

	public String getReason() {
		return reason;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}
}
