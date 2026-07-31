package com.example.labmanagement.registration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "PhieuGiangDay")
public class PhieuGiangDay {

	@Id
	@Column(name = "MaPhieu", nullable = false, length = 50)
	private String id;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "MaPhieu", nullable = false)
	private PhieuDangKy registration;

	@Column(name = "MaHocPhan", nullable = false, length = 50)
	private String courseId;

	@Column(name = "TenLopNhom", nullable = false, length = 100)
	private String classGroupName;

	protected PhieuGiangDay() {
	}

	public PhieuGiangDay(PhieuDangKy registration, String courseId, String classGroupName) {
		this.registration = registration;
		this.courseId = courseId;
		this.classGroupName = classGroupName;
	}

	public String getId() {
		return id;
	}
}
