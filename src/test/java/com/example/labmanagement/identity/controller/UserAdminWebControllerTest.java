package com.example.labmanagement.identity.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.labmanagement.common.web.NavigationModelAdvice;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.dto.AdminUserUpdateRequest;
import com.example.labmanagement.identity.dto.UserProfileResponse;
import com.example.labmanagement.identity.service.IdentityService;
import com.example.labmanagement.security.SecurityConfiguration;
import java.time.Instant;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@ContextConfiguration(classes = {UserAdminWebController.class, SecurityConfiguration.class,
		NavigationModelAdvice.class})
class UserAdminWebControllerTest {

	private static final String MANAGER_EMAIL = "manager@example.edu";
	private static final String ACCOUNT_ID = "SV900";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private IdentityService identityService;

	@Test
	void accountManagementIsVisibleOnlyToManagerAndSupportsFilters() throws Exception {
		UserProfileResponse pending = profile(NguoiDungTrangThai.CHO_DUYET);
		when(identityService.searchUsers(NguoiDungTrangThai.CHO_DUYET, "SV", "sv900", 0, 20))
				.thenReturn(new PageImpl<>(List.of(pending), PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/admin/users")).andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("**/login"));
		mockMvc.perform(get("/admin/users").with(user("student@example.edu").roles("SV")))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/admin/users").with(user(MANAGER_EMAIL).roles("CBQL")).param("keyword", "sv900")
				.param("role", "SV").param("status", "CHO_DUYET")).andExpect(status().isOk())
				.andExpect(view().name("identity/user-approvals"))
				.andExpect(content().string(Matchers.containsString("Quản lý tài khoản")))
				.andExpect(content().string(Matchers.containsString(ACCOUNT_ID)))
				.andExpect(content().string(Matchers.containsString("Phê duyệt")));
	}

	@Test
	void editPageLoadsCurrentAccountAndManagerCanUpdateIt() throws Exception {
		UserProfileResponse active = profile(NguoiDungTrangThai.HOAT_DONG);
		AdminUserUpdateRequest request = new AdminUserUpdateRequest("Sinh viên mới", "sv900@example.edu", "CNTT02",
				"GV", NguoiDungTrangThai.BI_KHOA, 0L);
		when(identityService.getUser(ACCOUNT_ID)).thenReturn(active);
		when(identityService.updateUser(ACCOUNT_ID, request))
				.thenReturn(new UserProfileResponse(ACCOUNT_ID, "Sinh viên mới", "sv900@example.edu", "CNTT02", "GV",
						NguoiDungTrangThai.BI_KHOA, 1, Instant.EPOCH, Instant.EPOCH));

		mockMvc.perform(get("/admin/users/{id}/edit", ACCOUNT_ID).with(user(MANAGER_EMAIL).roles("CBQL")))
				.andExpect(status().isOk()).andExpect(view().name("identity/user-form"))
				.andExpect(content().string(Matchers.containsString("Cập nhật tài khoản")))
				.andExpect(content().string(Matchers.containsString("sv900@example.edu")));

		mockMvc.perform(post("/admin/users/{id}", ACCOUNT_ID).with(user(MANAGER_EMAIL).roles("CBQL")).with(csrf())
				.param("fullName", "Sinh viên mới").param("email", "sv900@example.edu").param("classOrUnit", "CNTT02")
				.param("roleId", "GV").param("status", "BI_KHOA").param("version", "0"))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/users"));
		verify(identityService).updateUser(ACCOUNT_ID, request);
	}

	@Test
	void approvalRequiresManagerAndCsrfThenActivatesAccount() throws Exception {
		when(identityService.approveUser(ACCOUNT_ID, 0L)).thenReturn(profile(NguoiDungTrangThai.HOAT_DONG));

		mockMvc.perform(post("/admin/users/{id}/approve", ACCOUNT_ID).with(user(MANAGER_EMAIL).roles("CBQL"))
				.param("version", "0")).andExpect(status().isForbidden());
		mockMvc.perform(post("/admin/users/{id}/approve", ACCOUNT_ID).with(user("student@example.edu").roles("SV"))
				.with(csrf()).param("version", "0")).andExpect(status().isForbidden());
		verify(identityService, never()).approveUser(ACCOUNT_ID, 0L);

		mockMvc.perform(post("/admin/users/{id}/approve", ACCOUNT_ID).with(user(MANAGER_EMAIL).roles("CBQL"))
				.with(csrf()).param("version", "0")).andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/users"));
		verify(identityService).approveUser(ACCOUNT_ID, 0L);
	}

	@Test
	void lockAndUnlockRequireManagerAndCsrf() throws Exception {
		when(identityService.changeUserStatus(ACCOUNT_ID, NguoiDungTrangThai.BI_KHOA, 0L))
				.thenReturn(profile(NguoiDungTrangThai.BI_KHOA));

		mockMvc.perform(post("/admin/users/{id}/status", ACCOUNT_ID).with(user(MANAGER_EMAIL).roles("CBQL"))
				.param("status", "BI_KHOA").param("version", "0")).andExpect(status().isForbidden());
		mockMvc.perform(post("/admin/users/{id}/status", ACCOUNT_ID).with(user("student@example.edu").roles("SV"))
				.with(csrf()).param("status", "BI_KHOA").param("version", "0")).andExpect(status().isForbidden());
		verify(identityService, never()).changeUserStatus(ACCOUNT_ID, NguoiDungTrangThai.BI_KHOA, 0L);

		mockMvc.perform(post("/admin/users/{id}/status", ACCOUNT_ID).with(user(MANAGER_EMAIL).roles("CBQL"))
				.with(csrf()).param("status", "BI_KHOA").param("version", "0")).andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/users"));
		verify(identityService).changeUserStatus(ACCOUNT_ID, NguoiDungTrangThai.BI_KHOA, 0L);
	}

	private UserProfileResponse profile(NguoiDungTrangThai status) {
		return new UserProfileResponse(ACCOUNT_ID, "Sinh viên mới", "sv900@example.edu", "CNTT01", "SV", status, 0,
				Instant.EPOCH, Instant.EPOCH);
	}
}
