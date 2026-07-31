package com.example.labmanagement.maintenance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class BaoTriSuCoId implements Serializable {

	@Column(name = "MaBaoTri", nullable = false, length = 50)
	private String maintenanceId;

	@Column(name = "MaSuCo", nullable = false, length = 50)
	private String incidentId;

	protected BaoTriSuCoId() {
	}

	public BaoTriSuCoId(String maintenanceId, String incidentId) {
		this.maintenanceId = maintenanceId;
		this.incidentId = incidentId;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof BaoTriSuCoId that)) {
			return false;
		}
		return Objects.equals(maintenanceId, that.maintenanceId) && Objects.equals(incidentId, that.incidentId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(maintenanceId, incidentId);
	}
}
