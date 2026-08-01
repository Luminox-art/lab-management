package com.example.labmanagement.reporting.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.labmanagement.common.error.GlobalExceptionHandler;
import com.example.labmanagement.incident.domain.MucDoSuCo;
import com.example.labmanagement.reporting.application.DashboardFrequencyResponse;
import com.example.labmanagement.reporting.application.DashboardResponse;
import com.example.labmanagement.reporting.application.DashboardService;
import com.example.labmanagement.reporting.application.DashboardSeverityResponse;
import com.example.labmanagement.reporting.application.DashboardSummaryResponse;
import com.example.labmanagement.reporting.domain.DashboardGroup;
import com.example.labmanagement.security.SecurityConfiguration;
import java.time.LocalDate;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@ContextConfiguration(classes = {DashboardRestController.class, DashboardWebController.class,
		SecurityConfiguration.class, GlobalExceptionHandler.class})
class DashboardControllerTest {

	private static final LocalDate FROM = LocalDate.of(2035, 1, 1);
	private static final LocalDate TO = LocalDate.of(2035, 1, 31);
	@Autowired
	private MockMvc mockMvc;
	@MockitoBean
	private DashboardService dashboardService;

	@Test
	void apiIsManagerOnlyAndReturnsActualSessionMetrics() throws Exception {
		when(dashboardService.dashboard("cb@lab.local", FROM, TO, DashboardGroup.PHONG)).thenReturn(response());

		mockMvc.perform(get("/api/v1/dashboard?from=2035-01-01&to=2035-01-31&groupBy=PHONG")
				.with(user("gv@lab.local").roles("GV"))).andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/dashboard?from=2035-01-01&to=2035-01-31&groupBy=PHONG")
				.with(user("cb@lab.local").roles("CBQL"))).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.summary.actualSessions").value(8))
				.andExpect(jsonPath("$.data.frequencies[0].id").value("P0601"));
	}

	@Test
	void webDashboardRendersFilterMetricsBarsAndTextFallback() throws Exception {
		when(dashboardService.dashboard("cb@lab.local", FROM, TO, DashboardGroup.PHONG)).thenReturn(response());

		mockMvc.perform(
				get("/dashboard?from=2035-01-01&to=2035-01-31&groupBy=PHONG").with(user("cb@lab.local").roles("CBQL")))
				.andExpect(status().isOk()).andExpect(view().name("reporting/dashboard"))
				.andExpect(content().string(Matchers.containsString("Dashboard CBQL")))
				.andExpect(content().string(Matchers.containsString("Tần suất theo phòng")))
				.andExpect(content().string(Matchers.containsString("Lab máy tính")))
				.andExpect(content().string(Matchers.containsString("aria-label=\"Lab máy tính: 8 phiên\"")));
	}

	private DashboardResponse response() {
		return new DashboardResponse(FROM, TO, DashboardGroup.PHONG,
				new DashboardSummaryResponse(8, 6, 2, 75.0, 2, 20.0, 3, 1),
				List.of(new DashboardFrequencyResponse(DashboardGroup.PHONG, "P0601", "Lab máy tính", 8, 100)),
				List.of(new DashboardSeverityResponse(MucDoSuCo.CAO, 3, 100)), List.of());
	}
}
