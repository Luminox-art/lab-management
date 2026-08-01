package com.example.labmanagement.incident.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.labmanagement.catalog.domain.LoaiTaiNguyen;
import com.example.labmanagement.common.error.GlobalExceptionHandler;
import com.example.labmanagement.incident.application.IncidentResponse;
import com.example.labmanagement.incident.application.IncidentService;
import com.example.labmanagement.incident.domain.MucDoSuCo;
import com.example.labmanagement.incident.domain.SuCoTrangThai;
import com.example.labmanagement.security.SecurityConfiguration;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@ContextConfiguration(classes = {IncidentRestController.class, SecurityConfiguration.class,
		GlobalExceptionHandler.class})
class IncidentRestControllerTest {

	private static final String EMAIL = "gv-inc@lab.local";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private IncidentService incidentService;

	@Test
	void reportRequiresAuthenticationCsrfAndValidPayload() throws Exception {
		String valid = """
				{"resourceId":"TN-TB0001","sessionId":9,"severity":"CAO","description":"Mất nguồn"}
				""";
		when(incidentService.report(org.mockito.ArgumentMatchers.eq(EMAIL), any())).thenReturn(response());

		mockMvc.perform(post("/api/v1/incidents").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(valid))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/v1/incidents").with(user(EMAIL).roles("GV")).contentType(MediaType.APPLICATION_JSON)
				.content(valid)).andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/incidents").with(user(EMAIL).roles("GV")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(valid.replace("Mất nguồn", "   ")))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		mockMvc.perform(post("/api/v1/incidents").with(user(EMAIL).roles("GV")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(valid)).andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.status").value("MOI"));
	}

	@Test
	void patchIsRestrictedToManagerAndInvalidRequestDoesNotReachService() throws Exception {
		String valid = """
				{"handlerId":"CB001","status":"DANG_XU_LY","result":null,"version":0}
				""";
		mockMvc.perform(patch("/api/v1/incidents/SC-INC").with(user(EMAIL).roles("GV")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(valid)).andExpect(status().isForbidden());
		mockMvc.perform(patch("/api/v1/incidents/SC-INC").with(user("cb@lab.local").roles("CBQL")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(valid.replace("DANG_XU_LY", "")))
				.andExpect(status().isBadRequest());
		verify(incidentService, never()).update(any(), any(), any());
	}

	private IncidentResponse response() {
		return new IncidentResponse("SC-INC", 0, "TN-TB0001", LoaiTaiNguyen.THIET_BI, "TB0001", "Máy đo", 9L, "PDK-INC",
				"GV001", "Giảng viên", null, null, MucDoSuCo.CAO, "Mất nguồn", SuCoTrangThai.MOI,
				OffsetDateTime.parse("2026-08-01T09:00:00+07:00"), null, null);
	}
}
