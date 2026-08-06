package com.example.labmanagement.scheduling.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public class AdminBlockForm {

	@NotBlank
	private String resourceSelection;

	@NotNull
	private LocalDate startDate;

	@NotNull
	private LocalDate endDate;

	@Min(2)
	@Max(8)
	private Integer dayOfWeek;

	private List<Integer> periodIds;

	@NotBlank
	private String reason;

	public AdminBlockRequest toRequest() {
		String roomId = null;
		String deviceId = null;
		if (resourceSelection != null && resourceSelection.startsWith("ROOM:")) {
			roomId = resourceSelection.substring("ROOM:".length());
		} else if (resourceSelection != null && resourceSelection.startsWith("DEVICE:")) {
			deviceId = resourceSelection.substring("DEVICE:".length());
		}
		return new AdminBlockRequest(roomId, deviceId, startDate, endDate, dayOfWeek, periodIds, reason);
	}

	public String getResourceSelection() {
		return resourceSelection;
	}

	public void setResourceSelection(String resourceSelection) {
		this.resourceSelection = resourceSelection;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public Integer getDayOfWeek() {
		return dayOfWeek;
	}

	public void setDayOfWeek(Integer dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}

	public List<Integer> getPeriodIds() {
		return periodIds;
	}

	public void setPeriodIds(List<Integer> periodIds) {
		this.periodIds = periodIds;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}
}
