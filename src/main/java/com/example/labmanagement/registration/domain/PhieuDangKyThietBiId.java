package com.example.labmanagement.registration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class PhieuDangKyThietBiId implements Serializable {

	@Column(name = "MaPhieu", nullable = false, length = 50)
	private String registrationId;

	@Column(name = "MaThietBi", nullable = false, length = 50)
	private String deviceId;

	protected PhieuDangKyThietBiId() {
	}

	public PhieuDangKyThietBiId(String registrationId, String deviceId) {
		this.registrationId = registrationId;
		this.deviceId = deviceId;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof PhieuDangKyThietBiId that)) {
			return false;
		}
		return Objects.equals(registrationId, that.registrationId) && Objects.equals(deviceId, that.deviceId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(registrationId, deviceId);
	}
}
