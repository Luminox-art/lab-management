package com.example.labmanagement.registration.domain;

import com.example.labmanagement.identity.domain.NguoiDung;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "PhieuHuongDan")
public class PhieuHuongDan {

	@Id
	@Column(name = "MaPhieu", nullable = false, length = 50)
	private String id;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "MaPhieu", nullable = false)
	private PhieuDangKy registration;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "MaGVHuongDan", nullable = false)
	private NguoiDung instructor;

	protected PhieuHuongDan() {
	}

	public PhieuHuongDan(PhieuDangKy registration, NguoiDung instructor) {
		this.registration = registration;
		this.instructor = instructor;
	}

	public String getId() {
		return id;
	}
}
