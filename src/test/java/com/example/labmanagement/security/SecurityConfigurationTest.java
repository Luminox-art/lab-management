package com.example.labmanagement.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(SecurityConfigurationTest.TestController.class)
@Import({SecurityConfiguration.class, SecurityConfigurationTest.TestController.class})
class SecurityConfigurationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void protectedEndpointRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/secured")).andExpect(status().is3xxRedirection());
	}

	@Test
	void stateChangingRequestRequiresCsrfToken() throws Exception {
		mockMvc.perform(post("/secured").with(user("tester"))).andExpect(status().isForbidden());
		mockMvc.perform(post("/secured").with(user("tester")).with(csrf())).andExpect(status().isOk());
	}

	@Test
	void apiUsesJsonAuthenticationAndAccessDeniedResponses() throws Exception {
		mockMvc.perform(get("/api/test")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
		mockMvc.perform(get("/api/v1/users").with(user("lecturer").roles("GV"))).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
		mockMvc.perform(get("/api/v1/users").with(user("manager").roles("CBQL"))).andExpect(status().isOk());
	}

	@RestController
	static class TestController {

		@GetMapping("/secured")
		String get() {
			return "ok";
		}

		@GetMapping({"/api/test", "/api/v1/users"})
		String apiGet() {
			return "ok";
		}

		@PostMapping("/secured")
		String post() {
			return "ok";
		}
	}
}
