package com.example.labmanagement.usage.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.labmanagement.common.error.GlobalExceptionHandler;
import com.example.labmanagement.security.SecurityConfiguration;
import com.example.labmanagement.usage.application.SessionDeviceResponse;
import com.example.labmanagement.usage.application.UsageSessionResponse;
import com.example.labmanagement.usage.application.UsageSessionService;
import com.example.labmanagement.usage.domain.PhienSuDungTrangThai;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@ContextConfiguration(classes = {UsageSessionRestController.class, SecurityConfiguration.class,
		GlobalExceptionHandler.class})
class UsageSessionRestControllerTest {

	private static final String EMAIL = "gv-use@lab.local";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UsageSessionService sessionService;

	@Test
	void checkInRequiresAuthenticationCsrfAndValidDeviceConditions() throws Exception {
		when(sessionService.checkIn(org.mockito.ArgumentMatchers.eq(EMAIL), org.mockito.ArgumentMatchers.eq(1L), any()))
				.thenReturn(response(PhienSuDungTrangThai.DANG_SU_DUNG));
		String valid = """
				{"version":0,"devices":[{"deviceId":"TB-USE","condition":"Tốt","note":null}]}
				""";

		mockMvc.perform(
				post("/api/v1/sessions/1/check-in").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(valid))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/v1/sessions/1/check-in").with(user(EMAIL).roles("GV"))
				.contentType(MediaType.APPLICATION_JSON).content(valid)).andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/sessions/1/check-in").with(user(EMAIL).roles("GV")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(valid.replace("\"condition\":\"Tốt\"", "\"condition\":\"   \"")))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		mockMvc.perform(post("/api/v1/sessions/1/check-in").with(user(EMAIL).roles("GV")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(valid)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("DANG_SU_DUNG"));
		verify(sessionService).checkIn(org.mockito.ArgumentMatchers.eq(EMAIL), org.mockito.ArgumentMatchers.eq(1L),
				any());
	}

	@Test
	void invalidCheckoutDoesNotCallService() throws Exception {
		mockMvc.perform(post("/api/v1/sessions/1/check-out").with(user(EMAIL).roles("GV")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"version\":0,\"devices\":null,\"incidents\":[]}"))
				.andExpect(status().isBadRequest());
		verify(sessionService, never()).checkOut(any(), any(), any());
	}

	private UsageSessionResponse response(PhienSuDungTrangThai status) {
		return new UsageSessionResponse(1L, 1, "PDK-USE", LocalDate.of(2026, 8, 1), status, "P-USE", "Phòng dùng",
				"GV-USE", "Giảng viên", 1, "Tiết 1", LocalTime.of(7, 30), LocalTime.of(9, 0), null, null, null, null,
				List.of(new SessionDeviceResponse("TB-USE", "Máy đo", "Máy đo", "Tốt", null, null)), List.of(), false,
				false);
	}
}
