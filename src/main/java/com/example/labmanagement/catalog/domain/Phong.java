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
@Table(name = "Phong")
public class Phong extends VersionedEntity {

	@Id
	@Column(name = "MaPhong", nullable = false, length = 50)
	private String id;

	@Column(name = "TenPhong", nullable = false, length = 150)
	private String name;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "MaNhom", nullable = false)
	private NhomPhong group;

	@Column(name = "ViTri", nullable = false)
	private String location;

	@Column(name = "SucChua", nullable = false)
	private int capacity;

	@Enumerated(EnumType.STRING)
	@Column(name = "TrangThai", nullable = false, length = 20)
	private PhongTrangThai status;

	protected Phong() {
	}

	public Phong(String id, String name, NhomPhong group, String location, int capacity, PhongTrangThai status) {
		this.id = id;
		this.name = name;
		this.group = group;
		this.location = location;
		this.capacity = capacity;
		this.status = status;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public NhomPhong getGroup() {
		return group;
	}

	public String getLocation() {
		return location;
	}

	public int getCapacity() {
		return capacity;
	}

	public PhongTrangThai getStatus() {
		return status;
	}

	public void update(String name, NhomPhong group, String location, int capacity, PhongTrangThai status) {
		this.name = name;
		this.group = group;
		this.location = location;
		this.capacity = capacity;
		this.status = status;
	}

	public void startMaintenance() {
		this.status = PhongTrangThai.BAO_TRI;
	}

	public void finishMaintenance() {
		this.status = PhongTrangThai.SAN_SANG;
	}

	public void cancelMaintenance() {
		this.status = PhongTrangThai.SAN_SANG;
	}
}
