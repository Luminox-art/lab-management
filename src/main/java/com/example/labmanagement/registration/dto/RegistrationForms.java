package com.example.labmanagement.registration.dto;

import com.example.labmanagement.registration.domain.LoaiPhieu;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RegistrationForms {

	private RegistrationForms() {
	}

	public static List<ScheduleRange> scheduleRanges(List<RegistrationScheduleResponse> schedules) {
		if (schedules == null || schedules.isEmpty()) {
			return List.of();
		}
		List<RegistrationScheduleResponse> sorted = schedules.stream()
				.sorted(Comparator.comparingInt(RegistrationScheduleResponse::dayOfWeek)
						.thenComparingInt(RegistrationScheduleResponse::periodId))
				.toList();
		List<ScheduleRange> ranges = new ArrayList<>();
		for (RegistrationScheduleResponse schedule : sorted) {
			if (!ranges.isEmpty()) {
				ScheduleRange previous = ranges.getLast();
				if (previous.dayOfWeek() == schedule.dayOfWeek() && previous.endPeriodId() + 1 == schedule.periodId()) {
					ranges.set(ranges.size() - 1, previous.extend(schedule));
					continue;
				}
			}
			ranges.add(ScheduleRange.from(schedule));
		}
		return List.copyOf(ranges);
	}

	public static final class RegistrationForm {

		@NotNull
		private LoaiPhieu type;

		@NotBlank
		@Size(max = 2000)
		private String purpose;

		@NotBlank
		@Size(max = 50)
		private String roomId;

		@NotNull
		@Positive
		private Integer participantCount;

		@NotNull
		private LocalDate startDate;

		@NotNull
		private LocalDate endDate;

		@Valid
		@Size(min = 1, max = 128)
		private List<ScheduleForm> schedules = new ArrayList<>(List.of(new ScheduleForm()));

		@Size(max = 500)
		private List<String> deviceIds = new ArrayList<>();

		@Size(max = 50)
		private String courseCode;

		@Size(max = 100)
		private String classGroup;

		@Size(max = 50)
		private String supervisorId;

		@PositiveOrZero
		private Long version;

		public static RegistrationForm from(RegistrationResponse response) {
			RegistrationForm form = new RegistrationForm();
			form.type = response.type();
			form.purpose = response.purpose();
			form.roomId = response.roomId();
			form.participantCount = response.participantCount();
			form.startDate = response.startDate();
			form.endDate = response.endDate();
			form.schedules = scheduleRanges(response.schedules()).stream().map(ScheduleForm::from).toList();
			form.deviceIds = response.devices().stream().map(device -> device.id()).toList();
			form.courseCode = response.courseCode();
			form.classGroup = response.classGroup();
			form.supervisorId = response.supervisorId();
			form.version = response.version();
			return form;
		}

		public RegistrationFormRequest toRequest() {
			return new RegistrationFormRequest(type, purpose, roomId, participantCount, startDate, endDate,
					schedules.stream().flatMap(schedule -> schedule.toRequests().stream()).toList(), deviceIds,
					courseCode, classGroup, supervisorId, version);
		}

		public LoaiPhieu getType() {
			return type;
		}

		public void setType(LoaiPhieu type) {
			this.type = type;
		}

		public String getPurpose() {
			return purpose;
		}

		public void setPurpose(String purpose) {
			this.purpose = purpose;
		}

		public String getRoomId() {
			return roomId;
		}

		public void setRoomId(String roomId) {
			this.roomId = roomId;
		}

		public Integer getParticipantCount() {
			return participantCount;
		}

		public void setParticipantCount(Integer participantCount) {
			this.participantCount = participantCount;
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

		public List<ScheduleForm> getSchedules() {
			return schedules;
		}

		public void setSchedules(List<ScheduleForm> schedules) {
			this.schedules = schedules;
		}

		public List<String> getDeviceIds() {
			return deviceIds;
		}

		public void setDeviceIds(List<String> deviceIds) {
			this.deviceIds = deviceIds;
		}

		public String getCourseCode() {
			return courseCode;
		}

		public void setCourseCode(String courseCode) {
			this.courseCode = courseCode;
		}

		public String getClassGroup() {
			return classGroup;
		}

		public void setClassGroup(String classGroup) {
			this.classGroup = classGroup;
		}

		public String getSupervisorId() {
			return supervisorId;
		}

		public void setSupervisorId(String supervisorId) {
			this.supervisorId = supervisorId;
		}

		public Long getVersion() {
			return version;
		}

		public void setVersion(Long version) {
			this.version = version;
		}
	}

	public static final class ScheduleForm {

		@NotNull
		@Min(2)
		@Max(8)
		private Integer dayOfWeek;

		@NotNull
		@Positive
		private Integer startPeriodId;

		@NotNull
		@Positive
		private Integer endPeriodId;

		public ScheduleForm() {
		}

		public ScheduleForm(Integer dayOfWeek, Integer startPeriodId, Integer endPeriodId) {
			this.dayOfWeek = dayOfWeek;
			this.startPeriodId = startPeriodId;
			this.endPeriodId = endPeriodId;
		}

		private static ScheduleForm from(ScheduleRange range) {
			return new ScheduleForm(range.dayOfWeek(), range.startPeriodId(), range.endPeriodId());
		}

		public List<RegistrationScheduleRequest> toRequests() {
			List<RegistrationScheduleRequest> requests = new ArrayList<>();
			for (int periodId = startPeriodId; periodId <= endPeriodId; periodId++) {
				requests.add(new RegistrationScheduleRequest(dayOfWeek, periodId));
			}
			return List.copyOf(requests);
		}

		public Integer getDayOfWeek() {
			return dayOfWeek;
		}

		public void setDayOfWeek(Integer dayOfWeek) {
			this.dayOfWeek = dayOfWeek;
		}

		public Integer getStartPeriodId() {
			return startPeriodId;
		}

		public void setStartPeriodId(Integer startPeriodId) {
			this.startPeriodId = startPeriodId;
		}

		public Integer getEndPeriodId() {
			return endPeriodId;
		}

		public void setEndPeriodId(Integer endPeriodId) {
			this.endPeriodId = endPeriodId;
		}
	}

	public record ScheduleRange(int dayOfWeek, String dayOfWeekLabel, int startPeriodId, int endPeriodId,
			String startPeriodName, String endPeriodName, LocalTime startTime, LocalTime endTime, int periodCount) {

		private static ScheduleRange from(RegistrationScheduleResponse schedule) {
			return new ScheduleRange(schedule.dayOfWeek(), schedule.dayOfWeekLabel(), schedule.periodId(),
					schedule.periodId(), schedule.periodName(), schedule.periodName(), schedule.startTime(),
					schedule.endTime(), 1);
		}

		private ScheduleRange extend(RegistrationScheduleResponse schedule) {
			return new ScheduleRange(dayOfWeek, dayOfWeekLabel, startPeriodId, schedule.periodId(), startPeriodName,
					schedule.periodName(), startTime, schedule.endTime(), periodCount + 1);
		}

		public String periodLabel() {
			return startPeriodId == endPeriodId ? startPeriodName : startPeriodName + "–" + endPeriodName;
		}
	}

	public static final class CancellationForm {

		@NotBlank
		@Size(max = 255)
		private String reason;

		@NotNull
		@PositiveOrZero
		private Long version;

		public RegistrationCancelRequest toRequest() {
			return new RegistrationCancelRequest(reason, version);
		}

		public String getReason() {
			return reason;
		}

		public void setReason(String reason) {
			this.reason = reason;
		}

		public Long getVersion() {
			return version;
		}

		public void setVersion(Long version) {
			this.version = version;
		}
	}

	public static final class ApprovalForm {

		@Size(max = 500)
		private List<String> deviceIds = new ArrayList<>();

		@NotNull
		@PositiveOrZero
		private Long version;

		public ApprovalRequest toRequest() {
			return new ApprovalRequest(deviceIds, version);
		}

		public List<String> getDeviceIds() {
			return deviceIds;
		}

		public void setDeviceIds(List<String> deviceIds) {
			this.deviceIds = deviceIds;
		}

		public Long getVersion() {
			return version;
		}

		public void setVersion(Long version) {
			this.version = version;
		}
	}

	public static final class RejectionForm {

		@NotBlank
		@Size(max = 255)
		private String reason;

		@NotNull
		@PositiveOrZero
		private Long version;

		public RejectionRequest toRequest() {
			return new RejectionRequest(reason, version);
		}

		public String getReason() {
			return reason;
		}

		public void setReason(String reason) {
			this.reason = reason;
		}

		public Long getVersion() {
			return version;
		}

		public void setVersion(Long version) {
			this.version = version;
		}
	}
}
