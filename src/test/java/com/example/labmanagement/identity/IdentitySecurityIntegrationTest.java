package com.example.labmanagement.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.labmanagement.identity.domain.NguoiDung;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.domain.VaiTro;
import com.example.labmanagement.identity.repository.NguoiDungRepository;
import com.example.labmanagement.identity.repository.VaiTroRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "LAB_TEST_DB_PASSWORD", matches = ".+")
@Transactional
class IdentitySecurityIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private NguoiDungRepository userRepository;

	@Autowired
	private VaiTroRepository roleRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void registrationRequiresCsrfNormalizesInputAndStoresBcryptPendingAccount() throws Exception {
		String body = """
				{"identifier":" S2REG ","fullName":" Người đăng ký ","email":" S2REG@Example.EDU ",
				 "password":"strong-password","organization":" CNTT02 ","role":"SV"}
				""";

		mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isForbidden());
		mockMvc.perform(
				post("/api/v1/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.data.id").value("S2REG"))
				.andExpect(jsonPath("$.data.email").value("s2reg@example.edu"))
				.andExpect(jsonPath("$.data.status").value("CHO_DUYET"));

		NguoiDung saved = userRepository.findById("S2REG").orElseThrow();
		assertThat(passwordEncoder.matches("strong-password", saved.getPasswordHash())).isTrue();
		mockMvc.perform(
				post("/api/v1/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isConflict());
		mockMvc.perform(post("/api/v1/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(body.replace(" S2REG ", "S2REG-OTHER"))).andExpect(status().isConflict());
	}

	@Test
	void activeAccountLoginChangesSessionIdAndLogoutInvalidatesSession() throws Exception {
		createUser("S2ACTIVE", "s2active@example.edu", NguoiDungTrangThai.HOAT_DONG, "SV");
		MockHttpSession session = new MockHttpSession();
		String previousSessionId = session.getId();

		MvcResult login = mockMvc
				.perform(
						post("/api/v1/auth/login").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
								.content("{\"email\":\"s2active@example.edu\",\"password\":\"correct-password\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value("S2ACTIVE")).andReturn();

		HttpSession authenticatedSession = login.getRequest().getSession(false);
		assertThat(authenticatedSession).isNotNull();
		assertThat(authenticatedSession.getId()).isNotEqualTo(previousSessionId);
		assertThat(authenticatedSession.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
				.isNotNull();

		MvcResult logout = mockMvc
				.perform(post("/api/v1/auth/logout").session((MockHttpSession) authenticatedSession).with(csrf()))
				.andExpect(status().isNoContent()).andReturn();
		assertThat(logout.getRequest().getSession(false)).isNull();
	}

	@Test
	void pendingLockedUnknownAndWrongPasswordUseSameAuthenticationError() throws Exception {
		createUser("S2PENDING", "s2pending@example.edu", NguoiDungTrangThai.CHO_DUYET, "GV");
		createUser("S2LOCKED", "s2locked@example.edu", NguoiDungTrangThai.BI_KHOA, "GV");

		for (String body : new String[]{"{\"email\":\"s2pending@example.edu\",\"password\":\"correct-password\"}",
				"{\"email\":\"s2locked@example.edu\",\"password\":\"correct-password\"}",
				"{\"email\":\"missing@example.edu\",\"password\":\"correct-password\"}",
				"{\"email\":\"s2pending@example.edu\",\"password\":\"wrong-password\"}"}) {
			mockMvc.perform(
					post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
					.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
					.andExpect(jsonPath("$.message").value("Thông tin đăng nhập không hợp lệ."));
		}
	}

	@Test
	void profileUpdateAndPasswordChangeCannotAlterRoleOrStatus() throws Exception {
		createUser("S2PROFILE", "s2profile@example.edu", NguoiDungTrangThai.HOAT_DONG, "SV");

		mockMvc.perform(patch("/api/v1/me").with(user("s2profile@example.edu").roles("SV")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"fullName":"Tên mới","email":"NEWPROFILE@Example.EDU","classOrUnit":"CNTT03",
						 "roleId":"CBQL","status":"BI_KHOA"}
						""")).andExpect(status().isOk()).andExpect(jsonPath("$.data.roleId").value("SV"))
				.andExpect(jsonPath("$.data.status").value("HOAT_DONG"));

		NguoiDung updated = userRepository.findById("S2PROFILE").orElseThrow();
		assertThat(updated.getRole().getId()).isEqualTo("SV");
		assertThat(updated.getStatus()).isEqualTo(NguoiDungTrangThai.HOAT_DONG);
		mockMvc.perform(put("/api/v1/me/password").with(user("newprofile@example.edu").roles("SV")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\":\"wrong-password\",\"newPassword\":\"new-password\"}"))
				.andExpect(status().isUnprocessableEntity());
		mockMvc.perform(put("/api/v1/me/password").with(user("newprofile@example.edu").roles("SV")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"currentPassword\":\"correct-password\",\"newPassword\":\"new-password\"}"))
				.andExpect(status().isNoContent());
		assertThat(passwordEncoder.matches("new-password", updated.getPasswordHash())).isTrue();
	}

	@Test
	void onlyManagerCanFilterAndUpdateUsersWithOptimisticVersion() throws Exception {
		NguoiDung target = createUser("S2TARGET", "s2target@example.edu", NguoiDungTrangThai.CHO_DUYET, "SV");
		String update = """
				{"fullName":"Tài khoản đã duyệt","email":"s2target@example.edu","classOrUnit":"Khoa CNTT",
				 "roleId":"GV","status":"HOAT_DONG","version":0}
				""";

		mockMvc.perform(get("/api/v1/users").with(user("lecturer").roles("GV"))).andExpect(status().isForbidden());
		mockMvc.perform(patch("/api/v1/users/S2TARGET").with(user("lecturer").roles("GV")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(update)).andExpect(status().isForbidden());
		assertThat(target.getStatus()).isEqualTo(NguoiDungTrangThai.CHO_DUYET);

		mockMvc.perform(get("/api/v1/users").with(user("manager").roles("CBQL")).param("status", "CHO_DUYET")
				.param("role", "SV").param("keyword", "S2TARGET")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].id").value("S2TARGET"));
		mockMvc.perform(patch("/api/v1/users/S2TARGET").with(user("manager").roles("CBQL")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(update)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.roleId").value("GV"))
				.andExpect(jsonPath("$.data.status").value("HOAT_DONG"));
		assertThat(target.getVersion()).isEqualTo(1);

		mockMvc.perform(patch("/api/v1/users/S2TARGET").with(user("manager").roles("CBQL")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(update)).andExpect(status().isConflict());
		mockMvc.perform(patch("/api/v1/users/S2TARGET").with(user("manager").roles("CBQL")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(update.replace(",\"version\":0", "")))
				.andExpect(status().isBadRequest());
	}

	private NguoiDung createUser(String id, String email, NguoiDungTrangThai status, String roleId) {
		VaiTro role = roleRepository.findById(roleId).orElseThrow();
		return userRepository.saveAndFlush(new NguoiDung(id, "Người dùng Giai đoạn 2", email,
				passwordEncoder.encode("correct-password"), "Khoa CNTT", role, status));
	}
}
