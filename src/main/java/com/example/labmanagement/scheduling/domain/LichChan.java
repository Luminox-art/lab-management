package com.example.labmanagement.scheduling.domain;

import com.example.labmanagement.catalog.domain.TaiNguyen;
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
import java.time.LocalDate;

@Entity
@Table(name = "LichChan")
public class LichChan {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaLichChan", nullable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "MaTaiNguyen", nullable = false)
	private TaiNguyen resource;

	@Column(name = "NgayBatDau", nullable = false)
	private LocalDate startDate;

	@Column(name = "NgayKetThuc", nullable = false)
	private LocalDate endDate;

	@Column(name = "Thu")
	private Byte dayOfWeek;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaTiet")
	private TietHoc period;

	@Column(name = "LyDo", nullable = false, columnDefinition = "TEXT")
	private String reason;

	@Enumerated(EnumType.STRING)
	@Column(name = "TrangThai", nullable = false, length = 20)
	private LichChanTrangThai status;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "MaNguoiTao", nullable = false)
	private NguoiDung creator;

	protected LichChan() {
	}

	public LichChan(TaiNguyen resource, LocalDate startDate, LocalDate endDate, Byte dayOfWeek, TietHoc period,
			String reason, LichChanTrangThai status, NguoiDung creator) {
		this.resource = resource;
		this.startDate = startDate;
		this.endDate = endDate;
		this.dayOfWeek = dayOfWeek;
		this.period = period;
		this.reason = reason;
		this.status = status;
		this.creator = creator;
	}

	public Long getId() {
		return id;
	}

	public TaiNguyen getResource() {
		return resource;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public Byte getDayOfWeek() {
		return dayOfWeek;
	}

	public TietHoc getPeriod() {
		return period;
	}

	public String getReason() {
		return reason;
	}

	public LichChanTrangThai getStatus() {
		return status;
	}
}
