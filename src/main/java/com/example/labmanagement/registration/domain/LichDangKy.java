package com.example.labmanagement.registration.domain;

import com.example.labmanagement.scheduling.domain.TietHoc;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "LichDangKy")
public class LichDangKy {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaLich", nullable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "MaPhieu", nullable = false)
	private PhieuDangKy registration;

	@Column(name = "Thu", nullable = false)
	private byte dayOfWeek;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "MaTiet", nullable = false)
	private TietHoc period;

	protected LichDangKy() {
	}

	public LichDangKy(PhieuDangKy registration, int dayOfWeek, TietHoc period) {
		this.registration = registration;
		this.dayOfWeek = (byte) dayOfWeek;
		this.period = period;
	}

	public Long getId() {
		return id;
	}

	public PhieuDangKy getRegistration() {
		return registration;
	}

	public byte getDayOfWeek() {
		return dayOfWeek;
	}

	public TietHoc getPeriod() {
		return period;
	}
}
