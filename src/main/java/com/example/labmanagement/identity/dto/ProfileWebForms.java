package com.example.labmanagement.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ProfileWebForms {

	private ProfileWebForms() {
	}

	public static final class ProfileForm {

		@NotBlank
		@Size(max = 150)
		private String fullName;
		@NotBlank
		@Email
		@Size(max = 254)
		private String email;
		@Size(max = 150)
		private String classOrUnit;

		public static ProfileForm from(UserProfileResponse profile) {
			ProfileForm form = new ProfileForm();
			form.fullName = profile.fullName();
			form.email = profile.email();
			form.classOrUnit = profile.classOrUnit();
			return form;
		}

		public ProfileUpdateRequest toRequest() {
			return new ProfileUpdateRequest(fullName, email, classOrUnit);
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
	}

	public static final class PasswordForm {

		@NotBlank
		private String currentPassword;
		@NotBlank
		@Size(min = 8, max = 72)
		private String newPassword;
		@NotBlank
		private String confirmPassword;

		public String getCurrentPassword() {
			return currentPassword;
		}

		public void setCurrentPassword(String currentPassword) {
			this.currentPassword = currentPassword;
		}

		public String getNewPassword() {
			return newPassword;
		}

		public void setNewPassword(String newPassword) {
			this.newPassword = newPassword;
		}

		public String getConfirmPassword() {
			return confirmPassword;
		}

		public void setConfirmPassword(String confirmPassword) {
			this.confirmPassword = confirmPassword;
		}
	}
}
