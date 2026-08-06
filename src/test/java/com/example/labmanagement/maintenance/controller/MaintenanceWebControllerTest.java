package com.example.labmanagement.maintenance.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.labmanagement.catalog.domain.LoaiTaiNguyen;
import com.example.labmanagement.maintenance.domain.BaoTriTrangThai;
import com.example.labmanagement.maintenance.dto.MaintenanceAssigneeOptionResponse;
import com.example.labmanagement.maintenance.dto.MaintenanceProgressResponse;
import com.example.labmanagement.maintenance.dto.MaintenanceResourceOptionResponse;
import com.example.labmanagement.maintenance.dto.MaintenanceResponse;
import com.example.labmanagement.maintenance.service.MaintenanceService;
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
@ContextConfiguration(classes = {MaintenanceWebController.class, SecurityConfiguration.class})
class MaintenanceWebControllerTest {

	@Autowired
	private MockMvc mockMvc;
	@MockitoBean
	private MaintenanceService maintenanceService;

	@Test
	void listIsManagerOnlyAndRendersMaintenance() throws Exception {
		when(maintenanceService.search("cb@lab.local", null, null, null, null, 0, 20))
				.thenReturn(new PageImpl<>(List.of(response())));
		when(maintenanceService.resourceOptions()).thenReturn(List.of(resourceOption()));
		when(maintenanceService.assigneeOptions()).thenReturn(List.of(assigneeOption()));

		mockMvc.perform(get("/maintenances").with(user("gv@lab.local").roles("GV"))).andExpect(status().isForbidden());
		mockMvc.perform(get("/maintenances").with(user("cb@lab.local").roles("CBQL"))).andExpect(status().isOk())
				.andExpect(view().name("maintenance/list")).andExpect(content().string(Matchers.containsString("BT-1")))
				.andExpect(content().string(Matchers.containsString("Chờ xử lý")));
	}

	@Test
	void createFormKeepsIncidentAndResourceAndProvidesInlineErrors() throws Exception {
		when(maintenanceService.resourceOptions()).thenReturn(List.of(resourceOption()));
		when(maintenanceService.assigneeOptions()).thenReturn(List.of(assigneeOption()));

		mockMvc.perform(
				get("/maintenances/new?incidentId=SC-1&resourceId=TN-TB0001").with(user("cb@lab.local").roles("CBQL")))
				.andExpect(status().isOk()).andExpect(view().name("maintenance/form"))
				.andExpect(content().string(Matchers.containsString("value=\"SC-1\"")))
				.andExpect(content().string(Matchers.containsString("value=\"TN-TB0001\"")))
				.andExpect(content().string(Matchers.containsString("aria-invalid")))
				.andExpect(content().string(Matchers.containsString("name=\"_csrf\"")));
	}

	@Test
	void detailRendersChronologicalTimelineAndConfirmation() throws Exception {
		when(maintenanceService.get("cb@lab.local", "BT-1")).thenReturn(response());

		mockMvc.perform(get("/maintenances/BT-1").with(user("cb@lab.local").roles("CBQL"))).andExpect(status().isOk())
				.andExpect(view().name("maintenance/detail"))
				.andExpect(content().string(Matchers.containsString("Dòng thời gian tiến độ")))
				.andExpect(content().string(Matchers.containsString("Tiếp nhận")))
				.andExpect(content().string(Matchers.containsString("Xác nhận ghi nhận tiến độ bảo trì?")));
	}

	private MaintenanceResponse response() {
		OffsetDateTime time = OffsetDateTime.parse("2026-08-01T09:00:00+07:00");
		return new MaintenanceResponse("BT-1", 0, "TN-TB0001", LoaiTaiNguyen.THIET_BI, "TB0001", "Máy đo", "SC-1",
				"CB001", "Cán bộ", time, null, "Thay nguồn", BaoTriTrangThai.CHO_XU_LY, null,
				List.of(new MaintenanceProgressResponse(1L, time, BaoTriTrangThai.CHO_XU_LY, "Tiếp nhận", "CB001",
						"Cán bộ")));
	}

	private MaintenanceResourceOptionResponse resourceOption() {
		return new MaintenanceResourceOptionResponse("TN-TB0001", LoaiTaiNguyen.THIET_BI, "TB0001", "Máy đo");
	}

	private MaintenanceAssigneeOptionResponse assigneeOption() {
		return new MaintenanceAssigneeOptionResponse("CB001", "Cán bộ");
	}
}
