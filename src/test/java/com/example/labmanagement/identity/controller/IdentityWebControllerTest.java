package com.example.labmanagement.identity.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.dto.ProfileUpdateRequest;
import com.example.labmanagement.identity.dto.RegistrationRequest;
import com.example.labmanagement.identity.dto.UserProfileResponse;
import com.example.labmanagement.identity.service.IdentityService;
import com.example.labmanagement.security.SecurityConfiguration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IdentityWebController.class)
@Import(SecurityConfiguration.class)
class IdentityWebControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private IdentityService identityService;

	@Test
	void loginAndRegistrationPagesArePublic() throws Exception {
		mockMvc.perform(get("/login").param("error", "true")).andExpect(status().isOk()).andExpect(view().name("login"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Thông tin đăng nhập không hợp lệ")));
		mockMvc.perform(get("/register")).andExpect(status().isOk()).andExpect(view().name("register"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Giảng viên")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Sinh viên")));
	}

	@Test
	void validRegistrationFormCreatesPendingAccountAndRedirects() throws Exception {
		when(identityService.register(any(RegistrationRequest.class))).thenReturn(profile("student@example.edu"));

		mockMvc.perform(post("/register").with(csrf()).param("identifier", "SV900").param("fullName", "Sinh viên")
				.param("email", "student@example.edu").param("password", "strong-password")
				.param("organization", "CNTT01").param("role", "SV")).andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/registration-pending"));

		verify(identityService).register(any(RegistrationRequest.class));
	}

	@Test
	void profilePageRequiresAuthenticationAndUsesCurrentPrincipal() throws Exception {
		mockMvc.perform(get("/profile")).andExpect(status().is3xxRedirection());
		when(identityService.getProfile("student@example.edu")).thenReturn(profile("student@example.edu"));

		mockMvc.perform(get("/profile").with(user("student@example.edu").roles("SV"))).andExpect(status().isOk())
				.andExpect(view().name("profile"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Chỉnh sửa hồ sơ")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Đổi mật khẩu")));
		verify(identityService).getProfile("student@example.edu");
	}

	@Test
	void profileUpdateUsesCurrentPrincipalAndRedirects() throws Exception {
		when(identityService.updateProfile(eq("student@example.edu"), any(ProfileUpdateRequest.class)))
				.thenReturn(profile("student@example.edu"));

		mockMvc.perform(post("/profile").with(user("student@example.edu").roles("SV")).with(csrf())
				.param("fullName", "Tên mới").param("email", "student@example.edu").param("classOrUnit", "CNTT02"))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/profile?updated"));

		verify(identityService).updateProfile(eq("student@example.edu"),
				argThat(request -> request.fullName().equals("Tên mới") && request.classOrUnit().equals("CNTT02")));
	}

	@Test
	void profileEmailChangeLogsOutAndRequiresLoginWithNewEmail() throws Exception {
		when(identityService.updateProfile(eq("student@example.edu"), any(ProfileUpdateRequest.class)))
				.thenReturn(profile("new-email@example.edu"));

		mockMvc.perform(post("/profile").with(user("student@example.edu").roles("SV")).with(csrf())
				.param("fullName", "Sinh viên").param("email", "new-email@example.edu").param("classOrUnit", "CNTT01"))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?profileUpdated"));
	}

	@Test
	void passwordChangeRequiresMatchingConfirmationAndCurrentPrincipal() throws Exception {
		when(identityService.getProfile("student@example.edu")).thenReturn(profile("student@example.edu"));

		mockMvc.perform(post("/profile/password").with(user("student@example.edu").roles("SV")).with(csrf())
				.param("currentPassword", "old-password").param("newPassword", "new-password")
				.param("confirmPassword", "different-password")).andExpect(status().isOk())
				.andExpect(view().name("profile"))
				.andExpect(model().attributeHasFieldErrors("passwordForm", "confirmPassword"))
				.andExpect(content().string(
						org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("value=\"old-password\""))))
				.andExpect(content().string(
						org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("value=\"new-password\""))))
				.andExpect(content().string(org.hamcrest.Matchers
						.not(org.hamcrest.Matchers.containsString("value=\"different-password\""))));
		verify(identityService, never()).changePassword(any(), any());

		mockMvc.perform(post("/profile/password").with(user("student@example.edu").roles("SV")).with(csrf())
				.param("currentPassword", "old-password").param("newPassword", "new-password")
				.param("confirmPassword", "new-password")).andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/profile?passwordUpdated"));
		verify(identityService).changePassword(eq("student@example.edu"),
				argThat(request -> request.currentPassword().equals("old-password")
						&& request.newPassword().equals("new-password")));
	}

	@Test
	void profileChangesRequireCsrf() throws Exception {
		mockMvc.perform(post("/profile").with(user("student@example.edu").roles("SV")).param("fullName", "Tên mới")
				.param("email", "student@example.edu")).andExpect(status().isForbidden());
		mockMvc.perform(post("/profile/password").with(user("student@example.edu").roles("SV"))
				.param("currentPassword", "old-password").param("newPassword", "new-password")
				.param("confirmPassword", "new-password")).andExpect(status().isForbidden());
	}

	@Test
	void homeShowsActionsForCurrentRole() throws Exception {
		when(identityService.getProfile("student@example.edu")).thenReturn(profile("student@example.edu", "SV"));
		when(identityService.getProfile("manager@example.edu")).thenReturn(profile("manager@example.edu", "CBQL"));

		mockMvc.perform(get("/home").with(user("student@example.edu").roles("SV"))).andExpect(status().isOk())
				.andExpect(view().name("home"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Tạo phiếu mới"))).andExpect(content()
						.string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Quản trị vận hành"))));
		mockMvc.perform(get("/home").with(user("manager@example.edu").roles("CBQL"))).andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Quản lý tài khoản")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Quản trị vận hành")));
	}

	private UserProfileResponse profile(String email) {
		return profile(email, "SV");
	}

	private UserProfileResponse profile(String email, String role) {
		return new UserProfileResponse("SV900", "Người dùng", email, "CNTT01", role, NguoiDungTrangThai.HOAT_DONG, 0,
				Instant.EPOCH, Instant.EPOCH);
	}
}
