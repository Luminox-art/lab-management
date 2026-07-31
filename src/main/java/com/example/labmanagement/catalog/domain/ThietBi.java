package com.example.labmanagement.catalog.domain;

import com.example.labmanagement.common.metadata.VersionedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ThietBi")
public class ThietBi extends VersionedEntity {

	@Id
	@Column(name = "MaThietBi", nullable = false, length = 50)
	private String id;

	@Column(name = "TenThietBi", nullable = false, length = 150)
	private String name;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "MaLoai", nullable = false)
	private LoaiThietBi type;

	@Column(name = "SoSerial", unique = true, length = 100)
	private String serialNumber;

	@Column(name = "Model", length = 100)
	private String model;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaPhong")
	private Phong room;

	@Enumerated(EnumType.STRING)
	@Column(name = "TrangThai", nullable = false, length = 20)
	private ThietBiTrangThai status;

	protected ThietBi() {
	}

	public ThietBi(String id, String name, LoaiThietBi type, String serialNumber, String model, Phong room,
			ThietBiTrangThai status) {
		this.id = id;
		this.name = name;
		this.type = type;
		this.serialNumber = serialNumber;
		this.model = model;
		this.room = room;
		this.status = status;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public LoaiThietBi getType() {
		return type;
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	public String getModel() {
		return model;
	}

	public Phong getRoom() {
		return room;
	}

	public ThietBiTrangThai getStatus() {
		return status;
	}

	public void update(String name, LoaiThietBi type, String serialNumber, String model, Phong room,
			ThietBiTrangThai status) {
		this.name = name;
		this.type = type;
		this.serialNumber = serialNumber;
		this.model = model;
		this.room = room;
		this.status = status;
	}
}
