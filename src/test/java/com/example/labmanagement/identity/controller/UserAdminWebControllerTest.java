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
	void pendingAccountQueueIsVisibleOnlyToManager() throws Exception {
		UserProfileResponse pending = profile(NguoiDungTrangThai.CHO_DUYET);
		when(identityService.searchUsers(NguoiDungTrangThai.CHO_DUYET, null, "sv900", 0, 20))
				.thenReturn(new PageImpl<>(List.of(pending), PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/admin/users")).andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("**/login"));
		mockMvc.perform(get("/admin/users").with(user("student@example.edu").roles("SV")))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/admin/users").with(user(MANAGER_EMAIL).roles("CBQL")).param("keyword", "sv900"))
				.andExpect(status().isOk()).andExpect(view().name("identity/user-approvals"))
				.andExpect(content().string(Matchers.containsString("Duyệt tài khoản")))
				.andExpect(content().string(Matchers.containsString(ACCOUNT_ID)))
				.andExpect(content().string(Matchers.containsString("Phê duyệt")));
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

	private UserProfileResponse profile(NguoiDungTrangThai status) {
		return new UserProfileResponse(ACCOUNT_ID, "Sinh viên mới", "sv900@example.edu", "CNTT01", "SV", status, 0,
				Instant.EPOCH, Instant.EPOCH);
	}
}
