package com.example.labmanagement.registration.domain;

import com.example.labmanagement.catalog.domain.ThietBi;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "PhieuDangKyThietBi")
public class PhieuDangKyThietBi {

	@EmbeddedId
	private PhieuDangKyThietBiId id;

	@MapsId("registrationId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "MaPhieu", nullable = false)
	private PhieuDangKy registration;

	@MapsId("deviceId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "MaThietBi", nullable = false)
	private ThietBi device;

	@Column(name = "DaPhanBo", nullable = false)
	private boolean allocated;

	protected PhieuDangKyThietBi() {
	}

	public PhieuDangKyThietBi(PhieuDangKy registration, ThietBi device, boolean allocated) {
		this.id = new PhieuDangKyThietBiId(registration.getId(), device.getId());
		this.registration = registration;
		this.device = device;
		this.allocated = allocated;
	}

	public PhieuDangKyThietBiId getId() {
		return id;
	}
}
