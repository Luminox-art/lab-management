package com.example.labmanagement.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "LoaiThietBi")
public class LoaiThietBi {

	@Id
	@Column(name = "MaLoai", nullable = false, length = 50)
	private String id;

	@Column(name = "TenLoai", nullable = false, unique = true, length = 150)
	private String name;

	@Column(name = "YeuCauGVHuongDan", nullable = false)
	private boolean instructorRequired;

	@Column(name = "LaThietBiDiDong", nullable = false)
	private boolean mobile;

	@Column(name = "MoTa", columnDefinition = "TEXT")
	private String description;

	protected LoaiThietBi() {
	}

	public LoaiThietBi(String id, String name, boolean instructorRequired, boolean mobile, String description) {
		this.id = id;
		this.name = name;
		this.instructorRequired = instructorRequired;
		this.mobile = mobile;
		this.description = description;
	}

	public String getId() {
		return id;
	}
}
