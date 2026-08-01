package com.example.labmanagement.registration.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.labmanagement.common.error.GlobalExceptionHandler;
import com.example.labmanagement.registration.application.ApprovalService;
import com.example.labmanagement.registration.application.RegistrationDecisionResponse;
import com.example.labmanagement.registration.application.RegistrationFormRequest;
import com.example.labmanagement.registration.application.RegistrationResponse;
import com.example.labmanagement.registration.application.RegistrationService;
import com.example.labmanagement.registration.application.RegistrationSummaryResponse;
import com.example.labmanagement.registration.domain.LoaiPhieu;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import com.example.labmanagement.security.SecurityConfiguration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** MVC coverage for API-17..21 and registration response privacy. */
@WebMvcTest
@ContextConfiguration(classes = {RegistrationRestController.class, SecurityConfiguration.class,
		GlobalExceptionHandler.class})
class RegistrationRestControllerTest {

	private static final String INSTRUCTOR_EMAIL = "gv001@lab.local";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RegistrationService registrationService;

	@MockitoBean
	private ApprovalService approvalService;

	@Test
	void createRequiresAuthenticationCsrfAndCreatorRole() throws Exception {
		when(registrationService.create(any(), any())).thenReturn(response());

		mockMvc.perform(
				post("/api/v1/registrations").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(validJson()))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/v1/registrations").with(user(INSTRUCTOR_EMAIL).roles("GV"))
				.contentType(MediaType.APPLICATION_JSON).content(validJson())).andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/registrations").with(user("cb001@lab.local").roles("CBQL")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(validJson())).andExpect(status().isForbidden());

		mockMvc.perform(post("/api/v1/registrations").with(user(INSTRUCTOR_EMAIL).roles("GV")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(validJson())).andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.id").value("PDK-STAGE5"))
				.andExpect(jsonPath("$.data.status").value("CHO_DUYET"));
		verify(registrationService).create(org.mockito.ArgumentMatchers.eq(INSTRUCTOR_EMAIL), any());
	}

	@Test
	void invalidNestedScheduleReturnsStandardValidationError() throws Exception {
		mockMvc.perform(post("/api/v1/registrations").with(user(INSTRUCTOR_EMAIL).roles("GV")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(validJson().replace("\"dayOfWeek\":2", "\"dayOfWeek\":9"))).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		verify(registrationService, never()).create(any(), any());
	}

	@Test
	void listReturnsPaginationMetadataForAuthenticatedUser() throws Exception {
		RegistrationSummaryResponse summary = new RegistrationSummaryResponse("PDK-STAGE5", LoaiPhieu.GIANG_DAY,
				"Thực hành mạng", "P0601", "Phòng 6.1", 20, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 15),
				PhieuDangKyTrangThai.CHO_DUYET, 0, "GV001", "Giảng viên 01",
				OffsetDateTime.of(2026, 8, 1, 8, 0, 0, 0, ZoneOffset.ofHours(7)),
				OffsetDateTime.of(2026, 8, 1, 8, 0, 0, 0, ZoneOffset.ofHours(7)));
		when(registrationService.search(INSTRUCTOR_EMAIL, LoaiPhieu.GIANG_DAY, PhieuDangKyTrangThai.CHO_DUYET, null,
				null, null, 0, 20)).thenReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/api/v1/registrations").with(user(INSTRUCTOR_EMAIL).roles("GV")).param("type", "GIANG_DAY")
				.param("status", "CHO_DUYET")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].id").value("PDK-STAGE5"))
				.andExpect(jsonPath("$.meta.totalElements").value(1));
	}

	@Test
	void detailUpdateAndCancelUsePrincipalWithoutExposingSensitiveUserFields() throws Exception {
		when(registrationService.get(INSTRUCTOR_EMAIL, "PDK-STAGE5")).thenReturn(response());
		when(registrationService.update(org.mockito.ArgumentMatchers.eq(INSTRUCTOR_EMAIL),
				org.mockito.ArgumentMatchers.eq("PDK-STAGE5"), any())).thenReturn(response());
		when(registrationService.cancel(org.mockito.ArgumentMatchers.eq(INSTRUCTOR_EMAIL),
				org.mockito.ArgumentMatchers.eq("PDK-STAGE5"), any())).thenReturn(response());

		mockMvc.perform(get("/api/v1/registrations/PDK-STAGE5").with(user(INSTRUCTOR_EMAIL).roles("GV")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.creatorId").value("GV001"))
				.andExpect(content().string(Matchers.not(Matchers.containsString("password"))))
				.andExpect(content().string(Matchers.not(Matchers.containsString("creatorEmail"))));
		mockMvc.perform(put("/api/v1/registrations/PDK-STAGE5").with(user(INSTRUCTOR_EMAIL).roles("GV")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(validJsonWithVersion())).andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/registrations/PDK-STAGE5/cancel").with(user(INSTRUCTOR_EMAIL).roles("GV"))
				.with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"reason\":\"Không còn nhu cầu\",\"version\":0}")).andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/registrations/PDK-STAGE5/cancel").with(user("cb001@lab.local").roles("CBQL"))
				.with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"reason\":\"Không còn nhu cầu\",\"version\":0}")).andExpect(status().isForbidden());

		verify(registrationService).update(org.mockito.ArgumentMatchers.eq(INSTRUCTOR_EMAIL),
				org.mockito.ArgumentMatchers.eq("PDK-STAGE5"), any(RegistrationFormRequest.class));
	}

	@Test
	void approveAndRejectRequireManagerAndValidatedPayload() throws Exception {
		OffsetDateTime processedAt = OffsetDateTime.of(2026, 8, 1, 9, 0, 0, 0, ZoneOffset.ofHours(7));
		when(approvalService.approve(org.mockito.ArgumentMatchers.eq("cb001@lab.local"),
				org.mockito.ArgumentMatchers.eq("PDK-STAGE5"), any()))
				.thenReturn(new RegistrationDecisionResponse("PDK-STAGE5", PhieuDangKyTrangThai.DA_DUYET, 1,
						List.of("TB0001"), processedAt));
		when(approvalService.reject(org.mockito.ArgumentMatchers.eq("cb001@lab.local"),
				org.mockito.ArgumentMatchers.eq("PDK-STAGE5"), any()))
				.thenReturn(new RegistrationDecisionResponse("PDK-STAGE5", PhieuDangKyTrangThai.TU_CHOI, 1, List.of(),
						processedAt));

		mockMvc.perform(post("/api/v1/registrations/PDK-STAGE5/approve").with(user(INSTRUCTOR_EMAIL).roles("GV"))
				.with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"deviceIds\":[],\"version\":0}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/registrations/PDK-STAGE5/approve").with(user("cb001@lab.local").roles("CBQL"))
				.with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"deviceIds\":[\"TB0001\"],\"version\":0}")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("DA_DUYET"))
				.andExpect(jsonPath("$.data.allocatedDeviceIds[0]").value("TB0001"));
		mockMvc.perform(post("/api/v1/registrations/PDK-STAGE5/reject").with(user("cb001@lab.local").roles("CBQL"))
				.with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"   \",\"version\":0}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		mockMvc.perform(post("/api/v1/registrations/PDK-STAGE5/reject").with(user("cb001@lab.local").roles("CBQL"))
				.with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"reason\":\"Không phù hợp\",\"version\":0}")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("TU_CHOI"));
	}

	private RegistrationResponse response() {
		OffsetDateTime time = OffsetDateTime.of(2026, 8, 1, 8, 0, 0, 0, ZoneOffset.ofHours(7));
		return new RegistrationResponse("PDK-STAGE5", LoaiPhieu.GIANG_DAY, "Thực hành mạng", "P0601", "Phòng 6.1", 20,
				LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 15), PhieuDangKyTrangThai.CHO_DUYET, 0, "GV001",
				"Giảng viên 01", "INT1234", "01", null, null, List.of(), List.of(), List.of(), time, time, true, true);
	}

	private String validJson() {
		return """
				{"type":"GIANG_DAY","purpose":"Thực hành mạng","roomId":"P0601","participantCount":20,
				 "startDate":"2026-09-01","endDate":"2026-12-15",
				 "schedules":[{"dayOfWeek":2,"periodId":1}],"deviceIds":[],
				 "courseCode":"INT1234","classGroup":"01"}
				""";
	}

	private String validJsonWithVersion() {
		return validJson().replace("\"classGroup\":\"01\"}", "\"classGroup\":\"01\",\"version\":0}");
	}
}
