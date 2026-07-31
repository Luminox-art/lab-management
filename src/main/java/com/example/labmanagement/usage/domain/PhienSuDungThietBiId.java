package com.example.labmanagement.usage.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class PhienSuDungThietBiId implements Serializable {

	@Column(name = "MaPhien", nullable = false)
	private Long sessionId;

	@Column(name = "MaThietBi", nullable = false, length = 50)
	private String deviceId;

	protected PhienSuDungThietBiId() {
	}

	public PhienSuDungThietBiId(Long sessionId, String deviceId) {
		this.sessionId = sessionId;
		this.deviceId = deviceId;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof PhienSuDungThietBiId that)) {
			return false;
		}
		return Objects.equals(sessionId, that.sessionId) && Objects.equals(deviceId, that.deviceId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(sessionId, deviceId);
	}
}
