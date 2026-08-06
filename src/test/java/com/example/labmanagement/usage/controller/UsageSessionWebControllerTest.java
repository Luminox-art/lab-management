package com.example.labmanagement.usage.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.labmanagement.security.SecurityConfiguration;
import com.example.labmanagement.usage.domain.PhienSuDungTrangThai;
import com.example.labmanagement.usage.dto.SessionDeviceResponse;
import com.example.labmanagement.usage.dto.UsageSessionResponse;
import com.example.labmanagement.usage.service.UsageSessionService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@ContextConfiguration(classes = {UsageSessionWebController.class, SecurityConfiguration.class})
class UsageSessionWebControllerTest {

	private static final String EMAIL = "gv-use@lab.local";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UsageSessionService sessionService;

	@Test
	void listRequiresAuthenticationAndRendersOperationalStatus() throws Exception {
		when(sessionService.listAccessible(EMAIL, null, null)).thenReturn(List.of(response(true, false)));

		mockMvc.perform(get("/sessions")).andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("**/login"));
		mockMvc.perform(get("/sessions").with(user(EMAIL).roles("GV"))).andExpect(status().isOk())
				.andExpect(view().name("usage/list")).andExpect(content().string(Matchers.containsString("PDK-USE")))
				.andExpect(content().string(Matchers.containsString("Chưa bắt đầu")));
	}

	@Test
	void detailRendersAccessibleCheckInFormAndCsrfToken() throws Exception {
		when(sessionService.get(EMAIL, 1L)).thenReturn(response(true, false));

		mockMvc.perform(get("/sessions/1").with(user(EMAIL).roles("GV"))).andExpect(status().isOk())
				.andExpect(view().name("usage/detail"))
				.andExpect(content().string(Matchers.containsString("Bàn giao và check-in")))
				.andExpect(content().string(Matchers.containsString("TB-USE")))
				.andExpect(content().string(Matchers.containsString("name=\"_csrf\"")));
	}

	private UsageSessionResponse response(boolean canCheckIn, boolean canCheckOut) {
		return new UsageSessionResponse(1L, 0, "PDK-USE", LocalDate.of(2026, 8, 1), PhienSuDungTrangThai.CHUA_BAT_DAU,
				"P-USE", "Phòng dùng", "GV-USE", "Giảng viên", 1, "Tiết 1", LocalTime.of(7, 30), LocalTime.of(9, 0),
				null, null, null, null,
				List.of(new SessionDeviceResponse("TB-USE", "Máy đo", "Máy đo", null, null, null)), List.of(),
				canCheckIn, canCheckOut);
	}
}
