package com.example.labmanagement.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.scheduling.application.AdminBlockCreationResponse;
import com.example.labmanagement.scheduling.application.AdminBlockRequest;
import com.example.labmanagement.scheduling.application.AdminBlockService;
import com.example.labmanagement.scheduling.application.AvailabilityConflictType;
import com.example.labmanagement.scheduling.application.SchedulingService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MySQL acceptance for API-24/25 and the shared approval/block lock protocol.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "LAB_TEST_DB_PASSWORD", matches = ".+")
class AdminBlockMySqlIntegrationTest {

	private static final String MANAGER_EMAIL = "cb001@lab.local";
	private static final LocalDate MONDAY = LocalDate.of(2040, 1, 2);

	@Autowired
	private AdminBlockService adminBlockService;

	@Autowired
	private SchedulingService schedulingService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private MockMvc mockMvc;

	@BeforeEach
	@AfterEach
	void cleanStageSevenRows() {
		jdbcTemplate.update("DELETE FROM LichChan WHERE LyDo LIKE 'S7-%'");
		jdbcTemplate.update("DELETE FROM LichDangKy WHERE MaPhieu LIKE 'S7-%'");
		jdbcTemplate.update("DELETE FROM PhieuDangKy WHERE MaPhieu LIKE 'S7-%'");
	}

	@Test
	void multiplePeriodsAndAllDayBlocksImmediatelyAffectAvailabilityAndCalendar() {
		AdminBlockCreationResponse periods = adminBlockService.create(MANAGER_EMAIL,
				new AdminBlockRequest("P0605", null, MONDAY, MONDAY.plusWeeks(2), 2, List.of(1, 2), "S7-MULTI-PERIOD"));
		AdminBlockCreationResponse allDay = adminBlockService.create(MANAGER_EMAIL, new AdminBlockRequest("P0701", null,
				MONDAY.plusDays(1), MONDAY.plusDays(1), null, List.of(), "S7-ALL-DAY"));

		assertThat(periods.blocks()).hasSize(2).extracting(block -> block.periodId()).containsExactly(1, 2);
		assertThat(allDay.blocks()).singleElement().satisfies(block -> assertThat(block.periodId()).isNull());
		assertThat(schedulingService.checkAvailability("P0605", List.of(), MONDAY, MONDAY, 2, 1).conflicts())
				.anyMatch(conflict -> conflict.type() == AvailabilityConflictType.BLOCKED_SCHEDULE);
		assertThat(schedulingService.roomCalendar("P0605", MONDAY, MONDAY).events())
				.filteredOn(event -> event.type().name().equals("BLOCKED_SCHEDULE")).hasSize(2);
		assertThat(count("SELECT COUNT(*) FROM LichChan WHERE LyDo LIKE 'S7-%' AND TrangThai = 'HIEU_LUC'"))
				.isEqualTo(3);
	}

	@Test
	void approvedRegistrationConflictRollsBackEveryRequestedPeriod() {
		jdbcTemplate.update("""
				INSERT INTO PhieuDangKy
				  (MaPhieu, MaNguoiTao, MaPhong, LoaiPhieu, MucDich, SoNguoi, NgayBatDau, NgayKetThuc, TrangThai)
				VALUES ('S7-REG-1', 'GV001', 'P0702', 'NGHIEN_CUU', 'S7 conflict', 10, ?, ?, 'DA_DUYET')
				""", MONDAY, MONDAY);
		jdbcTemplate.update("INSERT INTO LichDangKy (MaPhieu, Thu, MaTiet) VALUES ('S7-REG-1', 2, 1)");

		assertThatThrownBy(() -> adminBlockService.create(MANAGER_EMAIL,
				new AdminBlockRequest("P0702", null, MONDAY, MONDAY, 2, List.of(1, 2), "S7-CONFLICT")))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getStatus().value()).isEqualTo(409));
		assertThat(count("SELECT COUNT(*) FROM LichChan WHERE LyDo = 'S7-CONFLICT'"))
				.as("the transaction must not save the non-conflicting second period").isZero();
	}

	@Test
	void apiCreatesDeviceBlockAndDeleteOnlyCancelsIt() throws Exception {
		String body = """
				{"deviceId":"TB0010","startDate":"2040-01-02","endDate":"2040-01-02",
				 "dayOfWeek":2,"periodIds":[3],"reason":"S7-API-DEVICE"}
				""";
		String response = mockMvc
				.perform(post("/api/v1/admin-blocks").with(user(MANAGER_EMAIL).roles("CBQL")).with(csrf())
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.data.blocks[0].resourceType").value("THIET_BI"))
				.andExpect(jsonPath("$.data.blocks[0].resourceId").value("TB0010")).andReturn().getResponse()
				.getContentAsString();
		long id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).path("data").path("blocks")
				.get(0).path("id").asLong();

		mockMvc.perform(delete("/api/v1/admin-blocks/{id}", id).with(user(MANAGER_EMAIL).roles("CBQL")).with(csrf()))
				.andExpect(status().isNoContent());

		assertThat(count("SELECT COUNT(*) FROM LichChan WHERE MaLichChan = ?", id)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT TrangThai FROM LichChan WHERE MaLichChan = ?", String.class, id))
				.isEqualTo("DA_HUY");
		assertThatThrownBy(() -> adminBlockService.cancel(MANAGER_EMAIL, id)).isInstanceOfSatisfying(ApiException.class,
				exception -> assertThat(exception.getStatus().value()).isEqualTo(409));
	}

	private int count(String sql, Object... arguments) {
		return jdbcTemplate.queryForObject(sql, Integer.class, arguments);
	}
}
