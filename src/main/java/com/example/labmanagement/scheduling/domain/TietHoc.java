package com.example.labmanagement.scheduling.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;

@Entity
@Table(name = "TietHoc")
public class TietHoc {

	@Id
	@Column(name = "MaTiet", nullable = false)
	private Integer id;

	@Column(name = "TenTiet", nullable = false, unique = true, length = 50)
	private String name;

	@Column(name = "GioBatDau", nullable = false)
	private LocalTime startTime;

	@Column(name = "GioKetThuc", nullable = false)
	private LocalTime endTime;

	protected TietHoc() {
	}

	public TietHoc(Integer id, String name, LocalTime startTime, LocalTime endTime) {
		this.id = id;
		this.name = name;
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public LocalTime getEndTime() {
		return endTime;
	}
}
