package com.example.labmanagement.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "NhomPhong")
public class NhomPhong {

	@Id
	@Column(name = "MaNhom", nullable = false, length = 50)
	private String id;

	@Column(name = "TenNhom", nullable = false, unique = true, length = 150)
	private String name;

	@Column(name = "MoTa", columnDefinition = "TEXT")
	private String description;

	protected NhomPhong() {
	}

	public NhomPhong(String id, String name, String description) {
		this.id = id;
		this.name = name;
		this.description = description;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public void update(String name, String description) {
		this.name = name;
		this.description = description;
	}
}
