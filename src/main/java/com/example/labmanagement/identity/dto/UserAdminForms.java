package com.example.labmanagement.identity.dto;

import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public final class UserAdminForms {

	private UserAdminForms() {
	}

	public static final class UserForm {

		@NotBlank
		@Size(max = 150)
		private String fullName;
		@NotBlank
		@Email
		@Size(max = 254)
		private String email;
		@Size(max = 150)
		private String classOrUnit;
		@NotBlank
		@Pattern(regexp = "ADMIN|CBQL|GV|SV")
		private String roleId;
		@NotNull
		private NguoiDungTrangThai status;
		@NotNull
		@PositiveOrZero
		private Long version;

		public static UserForm from(UserProfileResponse user) {
			UserForm form = new UserForm();
			form.fullName = user.fullName();
			form.email = user.email();
			form.classOrUnit = user.classOrUnit();
			form.roleId = user.roleId();
			form.status = user.status();
			form.version = user.version();
			return form;
		}

		public AdminUserUpdateRequest toRequest() {
			return new AdminUserUpdateRequest(fullName, email, classOrUnit, roleId, status, version);
		}

		public String getFullName() {
			return fullName;
		}

		public void setFullName(String fullName) {
			this.fullName = fullName;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public String getClassOrUnit() {
			return classOrUnit;
		}

		public void setClassOrUnit(String classOrUnit) {
			this.classOrUnit = classOrUnit;
		}

		public String getRoleId() {
			return roleId;
		}

		public void setRoleId(String roleId) {
			this.roleId = roleId;
		}

		public NguoiDungTrangThai getStatus() {
			return status;
		}

		public void setStatus(NguoiDungTrangThai status) {
			this.status = status;
		}

		public Long getVersion() {
			return version;
		}

		public void setVersion(Long version) {
			this.version = version;
		}
	}
}
