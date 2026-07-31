package com.example.labmanagement.maintenance.domain;

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
@Table(name = "TienDoBaoTri")
public class TienDoBaoTri {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaTienDo", nullable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "MaBaoTri", nullable = false)
	private BaoTri maintenance;

	@Column(name = "ThoiDiem", nullable = false)
	private Instant occurredAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "TrangThai", nullable = false, length = 20)
	private BaoTriTrangThai status;

	@Column(name = "NoiDung", nullable = false, columnDefinition = "TEXT")
	private String content;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "MaNguoiCapNhat", nullable = false)
	private NguoiDung updatedBy;

	protected TienDoBaoTri() {
	}

	public TienDoBaoTri(BaoTri maintenance, Instant occurredAt, BaoTriTrangThai status, String content,
			NguoiDung updatedBy) {
		this.maintenance = maintenance;
		this.occurredAt = occurredAt;
		this.status = status;
		this.content = content;
		this.updatedBy = updatedBy;
	}

	public Long getId() {
		return id;
	}
}
