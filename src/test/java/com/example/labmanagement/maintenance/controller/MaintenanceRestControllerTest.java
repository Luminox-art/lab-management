package com.example.labmanagement.maintenance.controller;

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
import com.example.labmanagement.maintenance.domain.BaoTriTrangThai;
import com.example.labmanagement.maintenance.dto.MaintenanceResponse;
import com.example.labmanagement.maintenance.service.MaintenanceService;
import com.example.labmanagement.security.SecurityConfiguration;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@ContextConfiguration(classes = {MaintenanceRestController.class, SecurityConfiguration.class,
		GlobalExceptionHandler.class})
class MaintenanceRestControllerTest {

	@Autowired
	private MockMvc mockMvc;
	@MockitoBean
	private MaintenanceService maintenanceService;

	@Test
	void createRequiresManagerCsrfAndValidPayload() throws Exception {
		String valid = """
				{"resourceId":"TN-TB0001","incidentId":"SC-1","assigneeId":"CB001","content":"Thay nguồn"}
				""";
		when(maintenanceService.create(org.mockito.ArgumentMatchers.eq("cb@lab.local"), any())).thenReturn(response());

		mockMvc.perform(
				post("/api/v1/maintenances").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(valid))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/v1/maintenances").with(user("gv@lab.local").roles("GV")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(valid)).andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/maintenances").with(user("cb@lab.local").roles("CBQL")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(valid.replace("Thay nguồn", "   ")))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		mockMvc.perform(post("/api/v1/maintenances").with(user("cb@lab.local").roles("CBQL")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(valid)).andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.status").value("CHO_XU_LY"));
	}

	@Test
	void updateRejectsNonManagerAndInvalidPayloadBeforeService() throws Exception {
		String valid = """
				{"status":"DANG_BAO_TRI","progressContent":"Đang thay nguồn","endAt":null,"result":null,"version":0}
				""";
		mockMvc.perform(patch("/api/v1/maintenances/BT-1").with(user("gv@lab.local").roles("GV")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(valid)).andExpect(status().isForbidden());
		mockMvc.perform(patch("/api/v1/maintenances/BT-1").with(user("cb@lab.local").roles("CBQL")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(valid.replace("Đang thay nguồn", " ")))
				.andExpect(status().isBadRequest());
		verify(maintenanceService, never()).update(any(), any(), any());
	}

	private MaintenanceResponse response() {
		return new MaintenanceResponse("BT-1", 0, "TN-TB0001", LoaiTaiNguyen.THIET_BI, "TB0001", "Máy đo", "SC-1",
				"CB001", "Cán bộ", OffsetDateTime.parse("2026-08-01T09:00:00+07:00"), null, "Thay nguồn",
				BaoTriTrangThai.CHO_XU_LY, null, List.of());
	}
}
