package com.example.labmanagement.identity.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.labmanagement.identity.application.IdentityService;
import com.example.labmanagement.identity.application.RegistrationRequest;
import com.example.labmanagement.identity.application.UserProfileResponse;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
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
		mockMvc.perform(get("/register")).andExpect(status().isOk()).andExpect(view().name("register"));
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
				.andExpect(view().name("profile"));
		verify(identityService).getProfile("student@example.edu");
	}

	private UserProfileResponse profile(String email) {
		return new UserProfileResponse("SV900", "Sinh viên", email, "CNTT01", "SV", NguoiDungTrangThai.CHO_DUYET, 0,
				Instant.EPOCH, Instant.EPOCH);
	}
}
