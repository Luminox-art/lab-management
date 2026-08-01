package com.example.labmanagement.scheduling.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.labmanagement.catalog.application.CatalogService;
import com.example.labmanagement.scheduling.application.AdminBlockCreationResponse;
import com.example.labmanagement.scheduling.application.AdminBlockService;
import com.example.labmanagement.scheduling.application.SchedulingService;
import com.example.labmanagement.security.SecurityConfiguration;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminBlockWebController.class)
@Import(SecurityConfiguration.class)
class AdminBlockWebControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AdminBlockService adminBlockService;

	@MockitoBean
	private CatalogService catalogService;

	@MockitoBean
	private SchedulingService schedulingService;

	@BeforeEach
	void setUp() {
		when(adminBlockService.findAll("cb001@lab.local")).thenReturn(List.of());
		when(catalogService.roomsForFilter()).thenReturn(List.of());
		when(catalogService.selectableDevices()).thenReturn(List.of());
		when(schedulingService.periods()).thenReturn(List.of());
	}

	@Test
	void pageIsManagerOnlyAndRendersAccessibleForm() throws Exception {
		mockMvc.perform(get("/admin-blocks").with(user("gv001@lab.local").roles("GV")))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/admin-blocks").with(user("cb001@lab.local").roles("CBQL"))).andExpect(status().isOk())
				.andExpect(view().name("schedule/admin-blocks"))
				.andExpect(content().string(Matchers.containsString("Tạo lịch chặn")))
				.andExpect(content().string(Matchers.containsString("Không chọn tiết để chặn cả ngày")))
				.andExpect(content().string(Matchers.containsString("Bỏ qua điều hướng")));
	}

	@Test
	void validFormCreatesMultiplePeriodRequestAndRedirects() throws Exception {
		when(adminBlockService.create(org.mockito.ArgumentMatchers.eq("cb001@lab.local"), any()))
				.thenReturn(new AdminBlockCreationResponse(List.of()));

		mockMvc.perform(post("/admin-blocks").with(user("cb001@lab.local").roles("CBQL")).with(csrf())
				.param("resourceSelection", "ROOM:P0601").param("startDate", "2035-01-01")
				.param("endDate", "2035-01-31").param("dayOfWeek", "2").param("periodIds", "1", "2")
				.param("reason", "Bảo trì định kỳ")).andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin-blocks"));
		verify(adminBlockService).create(org.mockito.ArgumentMatchers.eq("cb001@lab.local"), any());
	}

	@Test
	void cancelUsesPostCsrfAndRedirects() throws Exception {
		mockMvc.perform(post("/admin-blocks/8/cancel").with(user("cb001@lab.local").roles("CBQL")).with(csrf()))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin-blocks"));
		verify(adminBlockService).cancel("cb001@lab.local", 8L);
	}
}
