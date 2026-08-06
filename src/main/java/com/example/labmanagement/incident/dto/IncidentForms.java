package com.example.labmanagement.incident.dto;

import com.example.labmanagement.incident.domain.MucDoSuCo;
import com.example.labmanagement.incident.domain.SuCoTrangThai;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class IncidentForms {

	private IncidentForms() {
	}

	public static class ReportForm {

		@NotBlank
		@Size(max = 50)
		private String resourceId;
		@Positive
		private Long sessionId;
		@NotNull
		private MucDoSuCo severity = MucDoSuCo.TRUNG_BINH;
		@NotBlank
		@Size(max = 2000)
		private String description;

		public IncidentCreateRequest toRequest() {
			return new IncidentCreateRequest(resourceId, sessionId, severity, description);
		}

		public String getResourceId() {
			return resourceId;
		}

		public void setResourceId(String resourceId) {
			this.resourceId = resourceId;
		}

		public Long getSessionId() {
			return sessionId;
		}

		public void setSessionId(Long sessionId) {
			this.sessionId = sessionId;
		}

		public MucDoSuCo getSeverity() {
			return severity;
		}

		public void setSeverity(MucDoSuCo severity) {
			this.severity = severity;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}

	public static class UpdateForm {

		private String handlerId;
		@NotNull
		private SuCoTrangThai status;
		@Size(max = 2000)
		private String result;
		@NotNull
		private Long version;

		public static UpdateForm from(IncidentResponse incident) {
			UpdateForm form = new UpdateForm();
			form.handlerId = incident.handlerId();
			form.status = incident.status() == SuCoTrangThai.MOI ? SuCoTrangThai.DANG_XU_LY : incident.status();
			form.result = incident.result();
			form.version = incident.version();
			return form;
		}

		public IncidentUpdateRequest toRequest() {
			return new IncidentUpdateRequest(handlerId, status, result, version);
		}

		public String getHandlerId() {
			return handlerId;
		}

		public void setHandlerId(String handlerId) {
			this.handlerId = handlerId;
		}

		public SuCoTrangThai getStatus() {
			return status;
		}

		public void setStatus(SuCoTrangThai status) {
			this.status = status;
		}

		public String getResult() {
			return result;
		}

		public void setResult(String result) {
			this.result = result;
		}

		public Long getVersion() {
			return version;
		}

		public void setVersion(Long version) {
			this.version = version;
		}
	}
}
