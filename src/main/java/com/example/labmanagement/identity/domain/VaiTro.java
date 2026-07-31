package com.example.labmanagement.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "VaiTro")
public class VaiTro {

	@Id
	@Column(name = "MaVaiTro", nullable = false, length = 10)
	private String id;

	@Column(name = "TenVaiTro", nullable = false, unique = true, length = 50)
	private String name;

	protected VaiTro() {
	}

	public VaiTro(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
