package com.example.labmanagement.catalog.domain;

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
@Table(name = "TaiNguyen")
public class TaiNguyen {

	@Id
	@Column(name = "MaTaiNguyen", nullable = false, length = 50)
	private String id;

	@Enumerated(EnumType.STRING)
	@Column(name = "LoaiTaiNguyen", nullable = false, length = 20)
	private LoaiTaiNguyen resourceType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaPhong")
	private Phong room;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaThietBi")
	private ThietBi device;

	@Column(name = "PhongUnique", insertable = false, updatable = false, length = 50)
	private String uniqueRoomId;

	@Column(name = "ThietBiUnique", insertable = false, updatable = false, length = 50)
	private String uniqueDeviceId;

	protected TaiNguyen() {
	}

	private TaiNguyen(String id, LoaiTaiNguyen resourceType, Phong room, ThietBi device) {
		this.id = id;
		this.resourceType = resourceType;
		this.room = room;
		this.device = device;
	}

	public static TaiNguyen forRoom(String id, Phong room) {
		return new TaiNguyen(id, LoaiTaiNguyen.PHONG, room, null);
	}

	public static TaiNguyen forDevice(String id, ThietBi device) {
		return new TaiNguyen(id, LoaiTaiNguyen.THIET_BI, null, device);
	}

	public String getId() {
		return id;
	}

	public LoaiTaiNguyen getResourceType() {
		return resourceType;
	}
}
