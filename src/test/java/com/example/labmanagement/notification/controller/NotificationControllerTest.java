package com.example.labmanagement.notification.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.labmanagement.common.error.GlobalExceptionHandler;
import com.example.labmanagement.notification.domain.NotificationType;
import com.example.labmanagement.notification.dto.NotificationResponse;
import com.example.labmanagement.notification.service.NotificationService;
import com.example.labmanagement.security.SecurityConfiguration;
import java.time.OffsetDateTime;
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
@ContextConfiguration(classes = {NotificationRestController.class, NotificationWebController.class,
		SecurityConfiguration.class, GlobalExceptionHandler.class})
class NotificationControllerTest {

	@Autowired
	private MockMvc mockMvc;
	@MockitoBean
	private NotificationService notificationService;

	@Test
	void apiRequiresAuthenticationAndReturnsPaginationMetadata() throws Exception {
		when(notificationService.notifications("gv@lab.local", true, 0, 20))
				.thenReturn(new PageImpl<>(List.of(response()), PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/api/v1/notifications?unreadOnly=true")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/notifications?unreadOnly=true").with(user("gv@lab.local").roles("GV")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data[0].type").value("SU_CO"))
				.andExpect(jsonPath("$.meta.totalElements").value(1));
	}

	@Test
	void webListRendersAccessibleNotificationAndEmptyFilterControl() throws Exception {
		when(notificationService.notifications("sv@lab.local", false, 0, 20))
				.thenReturn(new PageImpl<>(List.of(response()), PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/notifications").with(user("sv@lab.local").roles("SV"))).andExpect(status().isOk())
				.andExpect(view().name("notification/list"))
				.andExpect(content().string(Matchers.containsString("Sự cố liên quan")))
				.andExpect(content().string(Matchers.containsString("name=\"unreadOnly\"")))
				.andExpect(content().string(Matchers.containsString("Bỏ qua điều hướng")));
	}

	private NotificationResponse response() {
		return new NotificationResponse("INCIDENT-SC-1", NotificationType.SU_CO, "Sự cố liên quan — CAO",
				"SC-1 · MỚI: Mất nguồn", OffsetDateTime.parse("2035-01-15T09:00:00+07:00"), "/incidents/SC-1", true);
	}
}
