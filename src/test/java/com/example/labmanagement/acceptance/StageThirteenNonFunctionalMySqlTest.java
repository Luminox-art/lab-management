package com.example.labmanagement.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.labmanagement.identity.domain.NguoiDung;
import com.example.labmanagement.identity.domain.NguoiDungTrangThai;
import com.example.labmanagement.identity.repository.NguoiDungRepository;
import com.example.labmanagement.identity.repository.VaiTroRepository;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "LAB_SESSION_TIMEOUT=15m")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "LAB_TEST_DB_PASSWORD", matches = ".+")
@Transactional
class StageThirteenNonFunctionalMySqlTest {

	private static final String SESSION_USER_ID = "S13SESSION";
	private static final String SESSION_USER_EMAIL = "s13session@lab.local";
	private static final String SESSION_PASSWORD = "stage13-password";
	private static final long THREE_SECONDS_IN_NANOS = Duration.ofSeconds(3).toNanos();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ServerProperties serverProperties;

	@Autowired
	private NguoiDungRepository userRepository;

	@Autowired
	private VaiTroRepository roleRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void configuredTimeoutAndExpiredSessionRequireAuthenticationAgain() throws Exception {
		assertThat(serverProperties.getServlet().getSession().getTimeout()).isEqualTo(Duration.ofMinutes(15));
		userRepository.saveAndFlush(new NguoiDung(SESSION_USER_ID, "Stage 13 Session", SESSION_USER_EMAIL,
				passwordEncoder.encode(SESSION_PASSWORD), "CNTT", roleRepository.findById("GV").orElseThrow(),
				NguoiDungTrangThai.HOAT_DONG));
		MockHttpSession anonymousSession = new MockHttpSession();
		MvcResult login = mockMvc.perform(post("/api/v1/auth/login").session(anonymousSession).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"email":"s13session@lab.local","password":"stage13-password"}
						""")).andExpect(status().isOk()).andReturn();
		MockHttpSession authenticatedSession = (MockHttpSession) login.getRequest().getSession(false);
		String expiredSessionId = authenticatedSession.getId();
		authenticatedSession.invalidate();

		mockMvc.perform(get("/api/v1/registrations").cookie(new Cookie("JSESSIONID", expiredSessionId)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void commonOperationP95StaysBelowThreeSecondsOnAcceptanceData() throws Exception {
		for (int index = 0; index < 10; index++) {
			performCommonOperation(index);
		}
		List<Long> durations = new ArrayList<>();
		for (int index = 0; index < 50; index++) {
			long startedAt = System.nanoTime();
			performCommonOperation(index);
			durations.add(System.nanoTime() - startedAt);
		}
		durations.sort(Long::compareTo);
		long p95 = durations.get((int) Math.ceil(durations.size() * 0.95) - 1);
		System.out.printf(Locale.ROOT, "Stage 13 acceptance p95: %.2f ms%n", p95 / 1_000_000.0);

		assertThat(p95).as("95%% common request latency").isLessThan(THREE_SECONDS_IN_NANOS);
	}

	private void performCommonOperation(int index) throws Exception {
		switch (index % 5) {
			case 0 -> mockMvc.perform(get("/api/v1/rooms").with(user("gv001@lab.local").roles("GV")).param("page", "0")
					.param("size", "20")).andExpect(status().isOk());
			case 1 -> mockMvc.perform(get("/api/v1/devices").with(user("gv001@lab.local").roles("GV"))
					.param("page", "0").param("size", "20")).andExpect(status().isOk());
			case 2 -> mockMvc.perform(get("/api/v1/registrations").with(user("gv001@lab.local").roles("GV"))
					.param("page", "0").param("size", "20")).andExpect(status().isOk());
			case 3 -> mockMvc
					.perform(get("/api/v1/availability").with(user("gv001@lab.local").roles("GV"))
							.param("roomId", "P0601").param("deviceIds", "TB0001").param("from", "2026-08-01")
							.param("to", "2026-08-01").param("dayOfWeek", "7").param("periodId", "2"))
					.andExpect(status().isOk());
			default -> mockMvc
					.perform(get("/api/v1/dashboard").with(user("cb001@lab.local").roles("CBQL"))
							.param("from", "2026-08-01").param("to", "2026-08-31").param("groupBy", "PHONG"))
					.andExpect(status().isOk());
		}
	}
}
