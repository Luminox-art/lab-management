package com.example.labmanagement.incident.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.labmanagement.catalog.domain.LoaiTaiNguyen;
import com.example.labmanagement.incident.domain.MucDoSuCo;
import com.example.labmanagement.incident.domain.SuCoTrangThai;
import com.example.labmanagement.incident.dto.IncidentHandlerOptionResponse;
import com.example.labmanagement.incident.dto.IncidentResourceOptionResponse;
import com.example.labmanagement.incident.dto.IncidentResponse;
import com.example.labmanagement.incident.service.IncidentService;
import com.example.labmanagement.security.SecurityConfiguration;
import java.time.OffsetDateTime;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@ContextConfiguration(classes = {IncidentWebController.class, SecurityConfiguration.class})
class IncidentWebControllerTest {

	private static final String EMAIL = "gv-inc@lab.local";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private IncidentService incidentService;

	@Test
	void listRequiresAuthenticationAndRendersSeverityAndStatusText() throws Exception {
		when(incidentService.search(EMAIL, null, null, null, null, null, 0, 20))
				.thenReturn(new PageImpl<>(List.of(response())));

		mockMvc.perform(get("/incidents")).andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("**/login"));
		mockMvc.perform(get("/incidents").with(user(EMAIL).roles("GV"))).andExpect(status().isOk())
				.andExpect(view().name("incident/list")).andExpect(content().string(Matchers.containsString("SC-INC")))
				.andExpect(content().string(Matchers.containsString("Nghiêm trọng")))
				.andExpect(content().string(Matchers.containsString("Mới")));
	}

	@Test
	void reportFormKeepsPrefilledSessionAndHasInlineErrorTargetsAndCsrf() throws Exception {
		when(incidentService.resourceOptions()).thenReturn(
				List.of(new IncidentResourceOptionResponse("TN-TB0001", LoaiTaiNguyen.THIET_BI, "TB0001", "Máy đo")));

		mockMvc.perform(get("/incidents/new?sessionId=9").with(user(EMAIL).roles("GV"))).andExpect(status().isOk())
				.andExpect(view().name("incident/form"))
				.andExpect(content().string(Matchers.containsString("name=\"sessionId\"")))
				.andExpect(content().string(Matchers.containsString("value=\"9\"")))
				.andExpect(content().string(Matchers.containsString("name=\"_csrf\"")))
				.andExpect(content().string(Matchers.containsString("aria-invalid")));
	}

	@Test
	void managerDetailRendersAssignmentStateAndConfirmation() throws Exception {
		when(incidentService.get("cb@lab.local", "SC-INC")).thenReturn(response());
		when(incidentService.handlerOptions())
				.thenReturn(List.of(new IncidentHandlerOptionResponse("CB001", "Cán bộ")));

		mockMvc.perform(get("/incidents/SC-INC").with(user("cb@lab.local").roles("CBQL"))).andExpect(status().isOk())
				.andExpect(view().name("incident/detail"))
				.andExpect(content().string(Matchers.containsString("Tiếp nhận và xử lý")))
				.andExpect(content().string(Matchers.containsString("CB001 — Cán bộ")))
				.andExpect(content().string(Matchers.containsString("Xác nhận cập nhật trạng thái sự cố?")));
	}

	private IncidentResponse response() {
		return new IncidentResponse("SC-INC", 0, "TN-TB0001", LoaiTaiNguyen.THIET_BI, "TB0001", "Máy đo", 9L, "PDK-INC",
				"GV001", "Giảng viên", null, null, MucDoSuCo.NGHIEM_TRONG, "Mất nguồn", SuCoTrangThai.MOI,
				OffsetDateTime.parse("2026-08-01T09:00:00+07:00"), null, null);
	}
}
