package com.example.labmanagement.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(StageThirteenEndToEndMySqlTest.FixedClockConfiguration.class)
@EnabledIfEnvironmentVariable(named = "LAB_TEST_DB_PASSWORD", matches = ".+")
class StageThirteenEndToEndMySqlTest {

	private static final String LECTURER_ID = "S13GV";
	private static final String LECTURER_EMAIL = "s13gv@lab.local";
	private static final String STUDENT_ID = "S13SV";
	private static final String STUDENT_EMAIL = "s13sv@lab.local";
	private static final String MANAGER_EMAIL = "cb001@lab.local";
	private static final String PASSWORD = "stage13-password";
	private static final LocalDate USAGE_DATE = LocalDate.of(2026, 8, 1);
	private static final Instant NOW = Instant.parse("2026-08-01T01:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	@AfterEach
	void cleanAcceptanceRows() {
		jdbcTemplate.update("""
				DELETE block FROM LichChan block
				JOIN BaoTri maintenance ON maintenance.MaBaoTri = block.MaBaoTri
				JOIN BaoTriSuCo source ON source.MaBaoTri = maintenance.MaBaoTri
				JOIN SuCo incident ON incident.MaSuCo = source.MaSuCo
				JOIN PhienSuDung session ON session.MaPhien = incident.MaPhien
				JOIN LichDangKy schedule ON schedule.MaLich = session.MaLich
				JOIN PhieuDangKy registration ON registration.MaPhieu = schedule.MaPhieu
				WHERE registration.MaNguoiTao IN (?, ?)
				""", LECTURER_ID, STUDENT_ID);
		jdbcTemplate.update("""
				DELETE maintenance FROM BaoTri maintenance
				JOIN BaoTriSuCo source ON source.MaBaoTri = maintenance.MaBaoTri
				JOIN SuCo incident ON incident.MaSuCo = source.MaSuCo
				JOIN PhienSuDung session ON session.MaPhien = incident.MaPhien
				JOIN LichDangKy schedule ON schedule.MaLich = session.MaLich
				JOIN PhieuDangKy registration ON registration.MaPhieu = schedule.MaPhieu
				WHERE registration.MaNguoiTao IN (?, ?)
				""", LECTURER_ID, STUDENT_ID);
		jdbcTemplate.update("""
				DELETE incident FROM SuCo incident
				JOIN PhienSuDung session ON session.MaPhien = incident.MaPhien
				JOIN LichDangKy schedule ON schedule.MaLich = session.MaLich
				JOIN PhieuDangKy registration ON registration.MaPhieu = schedule.MaPhieu
				WHERE registration.MaNguoiTao IN (?, ?)
				""", LECTURER_ID, STUDENT_ID);
		jdbcTemplate.update("""
				DELETE item FROM PhienSuDungThietBi item
				JOIN PhienSuDung session ON session.MaPhien = item.MaPhien
				JOIN LichDangKy schedule ON schedule.MaLich = session.MaLich
				JOIN PhieuDangKy registration ON registration.MaPhieu = schedule.MaPhieu
				WHERE registration.MaNguoiTao IN (?, ?)
				""", LECTURER_ID, STUDENT_ID);
		jdbcTemplate.update("""
				DELETE session FROM PhienSuDung session
				JOIN LichDangKy schedule ON schedule.MaLich = session.MaLich
				JOIN PhieuDangKy registration ON registration.MaPhieu = schedule.MaPhieu
				WHERE registration.MaNguoiTao IN (?, ?)
				""", LECTURER_ID, STUDENT_ID);
		jdbcTemplate.update("""
				DELETE history FROM XuLyPhieu history
				JOIN PhieuDangKy registration ON registration.MaPhieu = history.MaPhieu
				WHERE registration.MaNguoiTao IN (?, ?)
				""", LECTURER_ID, STUDENT_ID);
		jdbcTemplate.update("""
				DELETE teaching FROM PhieuGiangDay teaching
				JOIN PhieuDangKy registration ON registration.MaPhieu = teaching.MaPhieu
				WHERE registration.MaNguoiTao IN (?, ?)
				""", LECTURER_ID, STUDENT_ID);
		jdbcTemplate.update("""
				DELETE supervision FROM PhieuHuongDan supervision
				JOIN PhieuDangKy registration ON registration.MaPhieu = supervision.MaPhieu
				WHERE registration.MaNguoiTao IN (?, ?)
				""", LECTURER_ID, STUDENT_ID);
		jdbcTemplate.update("""
				DELETE allocation FROM PhieuDangKyThietBi allocation
				JOIN PhieuDangKy registration ON registration.MaPhieu = allocation.MaPhieu
				WHERE registration.MaNguoiTao IN (?, ?)
				""", LECTURER_ID, STUDENT_ID);
		jdbcTemplate.update("""
				DELETE schedule FROM LichDangKy schedule
				JOIN PhieuDangKy registration ON registration.MaPhieu = schedule.MaPhieu
				WHERE registration.MaNguoiTao IN (?, ?)
				""", LECTURER_ID, STUDENT_ID);
		jdbcTemplate.update("DELETE FROM PhieuDangKy WHERE MaNguoiTao IN (?, ?)", LECTURER_ID, STUDENT_ID);
		jdbcTemplate.update("DELETE FROM NguoiDung WHERE MaNguoiDung IN (?, ?)", LECTURER_ID, STUDENT_ID);
		jdbcTemplate.update("UPDATE ThietBi SET TrangThai = 'SAN_SANG' WHERE MaThietBi = 'TB0001'");
	}

	@Test
	void completeJourneyFromAccountRegistrationToActualUsageDashboard() throws Exception {
		register(LECTURER_ID, LECTURER_EMAIL, "GV");
		register(STUDENT_ID, STUDENT_EMAIL, "SV");
		activate(LECTURER_ID, LECTURER_EMAIL, "GV");
		activate(STUDENT_ID, STUDENT_EMAIL, "SV");
		MockHttpSession lecturerSession = login(LECTURER_EMAIL);

		mockMvc.perform(get("/api/v1/availability").session(lecturerSession).param("roomId", "P0601")
				.param("deviceIds", "TB0001").param("from", USAGE_DATE.toString()).param("to", USAGE_DATE.toString())
				.param("dayOfWeek", "7").param("periodId", "2")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.available").value(true));

		MvcResult registration = mockMvc
				.perform(post("/api/v1/registrations").session(lecturerSession).with(csrf())
						.contentType(MediaType.APPLICATION_JSON).content("""
								{"type":"GIANG_DAY","purpose":"Stage 13 acceptance journey","roomId":"P0601",
								 "participantCount":10,"startDate":"2026-08-01","endDate":"2026-08-01",
								 "schedules":[{"dayOfWeek":7,"periodId":2}],"deviceIds":["TB0001"],
								 "courseCode":"S13","classGroup":"01"}
								"""))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.data.status").value("CHO_DUYET")).andReturn();
		JsonNode registrationData = data(registration);
		String registrationId = registrationData.path("id").asText();
		long registrationVersion = registrationData.path("version").asLong();

		mockMvc.perform(post("/api/v1/registrations/{id}/approve", registrationId)
				.with(user(MANAGER_EMAIL).roles("CBQL")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"deviceIds\":[\"TB0001\"],\"version\":" + registrationVersion + "}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("DA_DUYET"))
				.andExpect(jsonPath("$.data.allocatedDeviceIds[0]").value("TB0001"));

		Long sessionId = jdbcTemplate.queryForObject("""
				SELECT session.MaPhien FROM PhienSuDung session
				JOIN LichDangKy schedule ON schedule.MaLich = session.MaLich
				WHERE schedule.MaPhieu = ?
				""", Long.class, registrationId);
		MvcResult checkIn = mockMvc
				.perform(post("/api/v1/sessions/{id}/check-in", sessionId).session(lecturerSession).with(csrf())
						.contentType(MediaType.APPLICATION_JSON).content("""
								{"version":0,"devices":[{"deviceId":"TB0001","condition":"Tot","note":"Du phu kien"}]}
								"""))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("DANG_SU_DUNG")).andReturn();
		long sessionVersion = data(checkIn).path("version").asLong();

		MvcResult checkOut = mockMvc
				.perform(post("/api/v1/sessions/{id}/check-out", sessionId).session(lecturerSession).with(csrf())
						.contentType(MediaType.APPLICATION_JSON).content("""
								{"version":%d,"devices":[{"deviceId":"TB0001","condition":"Hong nguon",
								 "note":"Khong khoi dong"}],"incidents":[{"deviceId":"TB0001","severity":"CAO",
								 "description":"Nguon thiet bi khong hoat dong"}]}
								""".formatted(sessionVersion)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("HOAN_THANH"))
				.andExpect(jsonPath("$.data.incidentIds.length()").value(1)).andReturn();
		String incidentId = data(checkOut).path("incidentIds").get(0).asText();

		MvcResult acceptedIncident = mockMvc
				.perform(patch("/api/v1/incidents/{id}", incidentId).with(user(MANAGER_EMAIL).roles("CBQL"))
						.with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content("{\"handlerId\":\"CB001\",\"status\":\"DANG_XU_LY\",\"result\":null,\"version\":0}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("DANG_XU_LY")).andReturn();
		long incidentVersion = data(acceptedIncident).path("version").asLong();

		MvcResult maintenance = mockMvc
				.perform(post("/api/v1/maintenances").with(user(MANAGER_EMAIL).roles("CBQL")).with(csrf())
						.contentType(MediaType.APPLICATION_JSON).content("""
								{"resourceId":"TN-TB0001","incidentId":"%s","assigneeId":"CB001",
								 "content":"Kiem tra va thay bo nguon"}
								""".formatted(incidentId)))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.data.status").value("CHO_XU_LY")).andReturn();
		JsonNode maintenanceData = data(maintenance);
		String maintenanceId = maintenanceData.path("id").asText();
		long maintenanceVersion = maintenanceData.path("version").asLong();

		MvcResult activeMaintenance = mockMvc
				.perform(patch("/api/v1/maintenances/{id}", maintenanceId).with(user(MANAGER_EMAIL).roles("CBQL"))
						.with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
								{"status":"DANG_BAO_TRI","progressContent":"Dang thay bo nguon","endAt":null,
								 "result":null,"version":%d}
								""".formatted(maintenanceVersion)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("DANG_BAO_TRI")).andReturn();
		long activeMaintenanceVersion = data(activeMaintenance).path("version").asLong();

		mockMvc.perform(patch("/api/v1/maintenances/{id}", maintenanceId).with(user(MANAGER_EMAIL).roles("CBQL"))
				.with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
						{"status":"HOAN_THANH","progressContent":"Da kiem tra lai","endAt":"2026-08-01T01:00:00Z",
						 "result":"Thiet bi hoat dong on dinh","version":%d}
						""".formatted(activeMaintenanceVersion))).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("HOAN_THANH"));
		mockMvc.perform(patch("/api/v1/incidents/{id}", incidentId).with(user(MANAGER_EMAIL).roles("CBQL")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"handlerId":"CB001","status":"DA_XU_LY","result":"Da thay bo nguon","version":%d}
						""".formatted(incidentVersion))).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("DA_XU_LY"));

		long actualSessions = jdbcTemplate.queryForObject("""
				SELECT COUNT(*) FROM PhienSuDung
				WHERE NgaySuDung = ? AND ThoiDiemCheckIn IS NOT NULL
				""", Long.class, USAGE_DATE);
		long incidents = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SuCo WHERE DATE(ThoiDiemBao) = ?",
				Long.class, USAGE_DATE);
		assertThat(actualSessions).isPositive();
		assertThat(incidents).isPositive();
		mockMvc.perform(get("/api/v1/dashboard").with(user(MANAGER_EMAIL).roles("CBQL"))
				.param("from", USAGE_DATE.toString()).param("to", USAGE_DATE.toString()).param("groupBy", "PHONG"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.summary.actualSessions").value(actualSessions))
				.andExpect(jsonPath("$.data.summary.incidents").value(incidents))
				.andExpect(jsonPath("$.data.frequencies[?(@.id == 'P0601')]").exists());
	}

	private void register(String id, String email, String role) throws Exception {
		mockMvc.perform(post("/api/v1/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
				{"identifier":"%s","fullName":"Stage 13 %s","email":"%s","password":"%s",
				 "organization":"CNTT","role":"%s"}
				""".formatted(id, role, email, PASSWORD, role))).andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.status").value("CHO_DUYET"));
	}

	private void activate(String id, String email, String role) throws Exception {
		mockMvc.perform(patch("/api/v1/users/{id}", id).with(user(MANAGER_EMAIL).roles("CBQL")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"fullName":"Stage 13 %s","email":"%s","classOrUnit":"CNTT","roleId":"%s",
						 "status":"HOAT_DONG","version":0}
						""".formatted(role, email, role))).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("HOAT_DONG"));
	}

	private MockHttpSession login(String email) throws Exception {
		MockHttpSession anonymousSession = new MockHttpSession();
		MvcResult result = mockMvc
				.perform(post("/api/v1/auth/login").session(anonymousSession).with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.email").value(email)).andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private JsonNode data(MvcResult result) throws Exception {
		return objectMapper.readTree(result.getResponse().getContentAsByteArray()).path("data");
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class FixedClockConfiguration {

		@Bean
		@Primary
		Clock stageThirteenClock() {
			return Clock.fixed(NOW, ZoneOffset.UTC);
		}
	}
}
