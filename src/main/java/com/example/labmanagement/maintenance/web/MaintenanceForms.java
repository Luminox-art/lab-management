package com.example.labmanagement.maintenance.web;

import com.example.labmanagement.common.clock.TimeConfiguration;
import com.example.labmanagement.maintenance.application.MaintenanceCreateRequest;
import com.example.labmanagement.maintenance.application.MaintenanceResponse;
import com.example.labmanagement.maintenance.application.MaintenanceUpdateRequest;
import com.example.labmanagement.maintenance.domain.BaoTriTrangThai;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

final class MaintenanceForms {

	private MaintenanceForms() {
	}

	static class CreateForm {
		@NotBlank
		@Size(max = 50)
		private String resourceId;
		@Size(max = 50)
		private String incidentId;
		@NotBlank
		@Size(max = 50)
		private String assigneeId;
		@NotBlank
		@Size(max = 2000)
		private String content;

		MaintenanceCreateRequest toRequest() {
			return new MaintenanceCreateRequest(resourceId, incidentId, assigneeId, content);
		}

		public String getResourceId() {
			return resourceId;
		}
		public void setResourceId(String resourceId) {
			this.resourceId = resourceId;
		}
		public String getIncidentId() {
			return incidentId;
		}
		public void setIncidentId(String incidentId) {
			this.incidentId = incidentId;
		}
		public String getAssigneeId() {
			return assigneeId;
		}
		public void setAssigneeId(String assigneeId) {
			this.assigneeId = assigneeId;
		}
		public String getContent() {
			return content;
		}
		public void setContent(String content) {
			this.content = content;
		}
	}

	static class UpdateForm {
		@NotNull
		private BaoTriTrangThai status;
		@NotBlank
		@Size(max = 2000)
		private String progressContent;
		private LocalDateTime endAt;
		@Size(max = 2000)
		private String result;
		@NotNull
		private Long version;

		static UpdateForm from(MaintenanceResponse maintenance) {
			UpdateForm form = new UpdateForm();
			form.status = maintenance.status() == BaoTriTrangThai.CHO_XU_LY
					? BaoTriTrangThai.DANG_BAO_TRI
					: maintenance.status();
			form.result = maintenance.result();
			form.version = maintenance.version();
			return form;
		}

		MaintenanceUpdateRequest toRequest() {
			return new MaintenanceUpdateRequest(status, progressContent,
					endAt == null ? null : endAt.atZone(TimeConfiguration.DISPLAY_ZONE).toInstant(), result, version);
		}

		public BaoTriTrangThai getStatus() {
			return status;
		}
		public void setStatus(BaoTriTrangThai status) {
			this.status = status;
		}
		public String getProgressContent() {
			return progressContent;
		}
		public void setProgressContent(String progressContent) {
			this.progressContent = progressContent;
		}
		public LocalDateTime getEndAt() {
			return endAt;
		}
		public void setEndAt(LocalDateTime endAt) {
			this.endAt = endAt;
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
