package com.example.labmanagement.usage.domain;

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
@Table(name = "PhienSuDungThietBi")
public class PhienSuDungThietBi {

	@EmbeddedId
	private PhienSuDungThietBiId id;

	@MapsId("sessionId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "MaPhien", nullable = false)
	private PhienSuDung session;

	@MapsId("deviceId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "MaThietBi", nullable = false)
	private ThietBi device;

	@Column(name = "TinhTrangNhan", nullable = false)
	private String receivedCondition;

	@Column(name = "TinhTrangTra")
	private String returnedCondition;

	@Column(name = "GhiChu", columnDefinition = "TEXT")
	private String note;

	protected PhienSuDungThietBi() {
	}

	public PhienSuDungThietBi(PhienSuDung session, ThietBi device, String receivedCondition, String returnedCondition,
			String note) {
		this.id = new PhienSuDungThietBiId(session.getId(), device.getId());
		this.session = session;
		this.device = device;
		this.receivedCondition = receivedCondition;
		this.returnedCondition = returnedCondition;
		this.note = note;
	}

	public PhienSuDungThietBiId getId() {
		return id;
	}

	public PhienSuDung getSession() {
		return session;
	}

	public ThietBi getDevice() {
		return device;
	}

	public String getReceivedCondition() {
		return receivedCondition;
	}

	public String getReturnedCondition() {
		return returnedCondition;
	}

	public String getNote() {
		return note;
	}

	public void recordReturn(String returnedCondition, String note) {
		this.returnedCondition = returnedCondition;
		this.note = note;
	}
}
