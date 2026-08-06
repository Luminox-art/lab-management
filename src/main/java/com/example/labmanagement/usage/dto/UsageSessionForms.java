package com.example.labmanagement.usage.dto;

import com.example.labmanagement.incident.domain.MucDoSuCo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

public final class UsageSessionForms {

	private UsageSessionForms() {
	}

	public static final class CheckInForm {

		@NotNull
		@PositiveOrZero
		private Long version;

		@Valid
		@Size(max = 500)
		private List<DeviceForm> devices = new ArrayList<>();

		public static CheckInForm from(UsageSessionResponse response) {
			CheckInForm form = new CheckInForm();
			form.version = response.version();
			form.devices = response.devices().stream().map(DeviceForm::forCheckIn).toList();
			return form;
		}

		public SessionCheckInRequest toRequest() {
			return new SessionCheckInRequest(version, devices.stream().map(DeviceForm::toCondition).toList());
		}

		public Long getVersion() {
			return version;
		}

		public void setVersion(Long version) {
			this.version = version;
		}

		public List<DeviceForm> getDevices() {
			return devices;
		}

		public void setDevices(List<DeviceForm> devices) {
			this.devices = devices;
		}
	}

	public static final class CheckOutForm {

		@NotNull
		@PositiveOrZero
		private Long version;

		@Valid
		@Size(max = 500)
		private List<DeviceForm> devices = new ArrayList<>();

		public static CheckOutForm from(UsageSessionResponse response) {
			CheckOutForm form = new CheckOutForm();
			form.version = response.version();
			form.devices = response.devices().stream().map(DeviceForm::forCheckOut).toList();
			return form;
		}

		public SessionCheckOutRequest toRequest() {
			List<SessionIncidentRequest> incidents = devices.stream().filter(DeviceForm::isReportIncident)
					.map(DeviceForm::toIncident).toList();
			return new SessionCheckOutRequest(version, devices.stream().map(DeviceForm::toCondition).toList(),
					incidents);
		}

		public Long getVersion() {
			return version;
		}

		public void setVersion(Long version) {
			this.version = version;
		}

		public List<DeviceForm> getDevices() {
			return devices;
		}

		public void setDevices(List<DeviceForm> devices) {
			this.devices = devices;
		}
	}

	public static final class DeviceForm {

		@NotBlank
		@Size(max = 50)
		private String deviceId;

		private String deviceName;

		@NotBlank
		@Size(max = 255)
		private String condition;

		@Size(max = 2000)
		private String note;

		private boolean reportIncident;

		private MucDoSuCo severity = MucDoSuCo.TRUNG_BINH;

		@Size(max = 2000)
		private String incidentDescription;

		static DeviceForm forCheckIn(SessionDeviceResponse device) {
			DeviceForm form = base(device);
			form.condition = device.receivedCondition() == null ? "Tốt" : device.receivedCondition();
			return form;
		}

		static DeviceForm forCheckOut(SessionDeviceResponse device) {
			DeviceForm form = base(device);
			form.condition = device.returnedCondition() == null ? "Tốt" : device.returnedCondition();
			return form;
		}

		private static DeviceForm base(SessionDeviceResponse device) {
			DeviceForm form = new DeviceForm();
			form.deviceId = device.id();
			form.deviceName = device.name();
			form.note = device.note();
			return form;
		}

		SessionDeviceConditionRequest toCondition() {
			return new SessionDeviceConditionRequest(deviceId, condition, note);
		}

		SessionIncidentRequest toIncident() {
			return new SessionIncidentRequest(deviceId, severity, incidentDescription);
		}

		public String getDeviceId() {
			return deviceId;
		}

		public void setDeviceId(String deviceId) {
			this.deviceId = deviceId;
		}

		public String getDeviceName() {
			return deviceName;
		}

		public void setDeviceName(String deviceName) {
			this.deviceName = deviceName;
		}

		public String getCondition() {
			return condition;
		}

		public void setCondition(String condition) {
			this.condition = condition;
		}

		public String getNote() {
			return note;
		}

		public void setNote(String note) {
			this.note = note;
		}

		public boolean isReportIncident() {
			return reportIncident;
		}

		public void setReportIncident(boolean reportIncident) {
			this.reportIncident = reportIncident;
		}

		public MucDoSuCo getSeverity() {
			return severity;
		}

		public void setSeverity(MucDoSuCo severity) {
			this.severity = severity;
		}

		public String getIncidentDescription() {
			return incidentDescription;
		}

		public void setIncidentDescription(String incidentDescription) {
			this.incidentDescription = incidentDescription;
		}
	}
}
