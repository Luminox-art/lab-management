package com.example.labmanagement.maintenance.domain;

import com.example.labmanagement.incident.domain.SuCo;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "BaoTriSuCo")
public class BaoTriSuCo {

	@EmbeddedId
	private BaoTriSuCoId id;

	@MapsId("maintenanceId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "MaBaoTri", nullable = false)
	private BaoTri maintenance;

	@MapsId("incidentId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "MaSuCo", nullable = false)
	private SuCo incident;

	protected BaoTriSuCo() {
	}

	public BaoTriSuCo(BaoTri maintenance, SuCo incident) {
		this.id = new BaoTriSuCoId(maintenance.getId(), incident.getId());
		this.maintenance = maintenance;
		this.incident = incident;
	}

	public BaoTriSuCoId getId() {
		return id;
	}
}
