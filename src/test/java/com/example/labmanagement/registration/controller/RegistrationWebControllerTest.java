package com.example.labmanagement.registration.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.labmanagement.catalog.domain.PhongTrangThai;
import com.example.labmanagement.catalog.dto.RoomResponse;
import com.example.labmanagement.catalog.service.CatalogService;
import com.example.labmanagement.registration.domain.HanhDongXuLyPhieu;
import com.example.labmanagement.registration.domain.LoaiPhieu;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import com.example.labmanagement.registration.dto.ApprovalPreviewResponse;
import com.example.labmanagement.registration.dto.ApprovalWarningResponse;
import com.example.labmanagement.registration.dto.ApprovalWarningType;
import com.example.labmanagement.registration.dto.RegistrationDeviceOptionResponse;
import com.example.labmanagement.registration.dto.RegistrationDeviceResponse;
import com.example.labmanagement.registration.dto.RegistrationFormRequest;
import com.example.labmanagement.registration.dto.RegistrationForms;
import com.example.labmanagement.registration.dto.RegistrationHistoryResponse;
import com.example.labmanagement.registration.dto.RegistrationResponse;
import com.example.labmanagement.registration.dto.RegistrationScheduleResponse;
import com.example.labmanagement.registration.dto.RegistrationSummaryResponse;
import com.example.labmanagement.registration.dto.SupervisorOptionResponse;
import com.example.labmanagement.registration.service.ApprovalService;
import com.example.labmanagement.registration.service.RegistrationService;
import com.example.labmanagement.scheduling.dto.PeriodResponse;
import com.example.labmanagement.scheduling.service.SchedulingService;
import com.example.labmanagement.security.SecurityConfiguration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@ContextConfiguration(classes = {RegistrationWebController.class, SecurityConfiguration.class})
class RegistrationWebControllerTest {

	private static final String USER_EMAIL = "sv001@lab.local";
	private static final String LECTURER_EMAIL = "gv001@lab.local";
	private static final String REGISTRATION_ID = "PDK-WEB";
	private static final OffsetDateTime CREATED_AT = OffsetDateTime.of(2026, 8, 1, 8, 0, 0, 0, ZoneOffset.ofHours(7));

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RegistrationService registrationService;

	@MockitoBean
	private CatalogService catalogService;

	@MockitoBean
	private SchedulingService schedulingService;

	@MockitoBean
	private ApprovalService approvalService;

	@BeforeEach
	void setUp() {
		RoomResponse room = new RoomResponse("P0601", "Phòng 6.1", "NP01", "Nhóm phòng máy", "Tầng 6", 40,
				PhongTrangThai.SAN_SANG, 0);
		when(catalogService.selectableRooms()).thenReturn(List.of(room));
		when(registrationService.deviceOptions()).thenReturn(List
				.of(new RegistrationDeviceOptionResponse("TB0001", "Robot", "Robot", true, true, "P0601", "P1001")));
		when(registrationService.supervisorOptions())
				.thenReturn(List.of(new SupervisorOptionResponse("GV001", "Giảng viên 01", "Khoa CNTT")));
		when(schedulingService.periods())
				.thenReturn(List.of(new PeriodResponse(1, "Tiết 1", LocalTime.of(7, 0), LocalTime.of(7, 50))));
	}

	@Test
	void registrationListRequiresAuthenticationAndRendersScopedResults() throws Exception {
		RegistrationSummaryResponse summary = new RegistrationSummaryResponse(REGISTRATION_ID, LoaiPhieu.HOC_TAP,
				"Thực hành mạng", "P0601", "Phòng 6.1", 10, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
				PhieuDangKyTrangThai.CHO_DUYET, 0, "SV001", "Sinh viên 01", CREATED_AT, CREATED_AT);
		when(registrationService.search(USER_EMAIL, null, null, null, null, null, 0, 20))
				.thenReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/registrations")).andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("**/login"));

		mockMvc.perform(get("/registrations").with(user(USER_EMAIL).roles("SV"))).andExpect(status().isOk())
				.andExpect(view().name("registration/list"))
				.andExpect(content().string(Matchers.containsString(REGISTRATION_ID)))
				.andExpect(content().string(Matchers.containsString("P0601")));
	}

	@Test
	void newRegistrationRendersStudentOptionsAndRejectsManager() throws Exception {
		mockMvc.perform(get("/registrations/new").with(user(USER_EMAIL).roles("SV"))).andExpect(status().isOk())
				.andExpect(view().name("registration/form"))
				.andExpect(content().string(Matchers.containsString("value=\"HOC_TAP\"")))
				.andExpect(content().string(Matchers.containsString("value=\"NGHIEN_CUU\"")))
				.andExpect(content().string(Matchers.not(Matchers.containsString("value=\"GIANG_DAY\""))))
				.andExpect(content().string(Matchers.containsString("TB0001")))
				.andExpect(content().string(Matchers.containsString("GV001")))
				.andExpect(content().string(Matchers.containsString("data-capacity=\"40\"")))
				.andExpect(content().string(Matchers.containsString("data-mobile=\"true\"")))
				.andExpect(content().string(Matchers.containsString("Đang sử dụng tại P1001")))
				.andExpect(content().string(Matchers.containsString("Phòng quản lý: P0601")))
				.andExpect(content().string(Matchers.containsString("data-availability-status")))
				.andExpect(content().string(Matchers.containsString("data-room-calendar-link")))
				.andExpect(content().string(Matchers.containsString("data-schedule-room-availability")));

		mockMvc.perform(get("/registrations/new").with(user("cb001@lab.local").roles("CBQL")))
				.andExpect(status().isForbidden());
	}

	@Test
	void createRegistrationBindsMultipleSchedulesAndRedirects() throws Exception {
		when(registrationService.create(eq(USER_EMAIL), any(RegistrationFormRequest.class))).thenReturn(response());

		mockMvc.perform(post("/registrations").with(user(USER_EMAIL).roles("SV")).with(csrf()).param("type", "HOC_TAP")
				.param("purpose", "Thực hành mạng").param("roomId", "P0601").param("participantCount", "20")
				.param("startDate", "2035-01-01").param("endDate", "2035-01-07").param("schedules[0].dayOfWeek", "2")
				.param("schedules[0].startPeriodId", "1").param("schedules[0].endPeriodId", "3")
				.param("schedules[1].dayOfWeek", "3").param("schedules[1].startPeriodId", "2")
				.param("schedules[1].endPeriodId", "2")).andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/registrations/" + REGISTRATION_ID));

		ArgumentCaptor<RegistrationFormRequest> requestCaptor = ArgumentCaptor.forClass(RegistrationFormRequest.class);
		verify(registrationService).create(eq(USER_EMAIL), requestCaptor.capture());
		assertThat(requestCaptor.getValue().schedules())
				.extracting(schedule -> schedule.dayOfWeek() + "|" + schedule.periodId())
				.containsExactly("2|1", "2|2", "2|3", "3|2");
	}

	@Test
	void overlappingScheduleRangesReturnInlineErrorWithoutCallingService() throws Exception {
		mockMvc.perform(post("/registrations").with(user(USER_EMAIL).roles("SV")).with(csrf()).param("type", "HOC_TAP")
				.param("purpose", "Thực hành mạng").param("roomId", "P0601").param("participantCount", "20")
				.param("startDate", "2035-01-01").param("endDate", "2035-01-07").param("schedules[0].dayOfWeek", "2")
				.param("schedules[0].startPeriodId", "1").param("schedules[0].endPeriodId", "3")
				.param("schedules[1].dayOfWeek", "2").param("schedules[1].startPeriodId", "3")
				.param("schedules[1].endPeriodId", "5")).andExpect(status().isOk())
				.andExpect(view().name("registration/form"))
				.andExpect(content().string(Matchers.containsString("Khoảng tiết này giao với một lịch khác.")));

		verify(registrationService, never()).create(any(), any());
	}

	@Test
	void scheduleRangeEndingBeforeStartReturnsInlineError() throws Exception {
		mockMvc.perform(post("/registrations").with(user(USER_EMAIL).roles("SV")).with(csrf()).param("type", "HOC_TAP")
				.param("purpose", "Thực hành mạng").param("roomId", "P0601").param("participantCount", "20")
				.param("startDate", "2035-01-01").param("endDate", "2035-01-07").param("schedules[0].dayOfWeek", "2")
				.param("schedules[0].startPeriodId", "3").param("schedules[0].endPeriodId", "1"))
				.andExpect(status().isOk()).andExpect(view().name("registration/form"))
				.andExpect(content().string(Matchers.containsString("Tiết kết thúc phải bằng hoặc sau tiết bắt đầu.")));

		verify(registrationService, never()).create(any(), any());
	}

	@Test
	void scheduleDayOutsideDateRangeReturnsInlineError() throws Exception {
		mockMvc.perform(post("/registrations").with(user(USER_EMAIL).roles("SV")).with(csrf()).param("type", "HOC_TAP")
				.param("purpose", "Thực hành mạng").param("roomId", "P0601").param("participantCount", "20")
				.param("startDate", "2035-01-01").param("endDate", "2035-01-01").param("schedules[0].dayOfWeek", "3")
				.param("schedules[0].startPeriodId", "1").param("schedules[0].endPeriodId", "3"))
				.andExpect(status().isOk()).andExpect(view().name("registration/form"))
				.andExpect(content().string(Matchers.containsString("Khoảng ngày không chứa thứ đã chọn.")));

		verify(registrationService, never()).create(any(), any());
	}

	@Test
	void managerQueueRendersExtendedFiltersAndTextWarnings() throws Exception {
		String managerEmail = "cb001@lab.local";
		LocalDate date = LocalDate.of(2035, 1, 1);
		RegistrationSummaryResponse summary = new RegistrationSummaryResponse(REGISTRATION_ID, LoaiPhieu.HOC_TAP,
				"Thực hành mạng", "P0601", "Phòng 6.1", 10, date, date, PhieuDangKyTrangThai.CHO_DUYET, 0, "SV001",
				"Sinh viên 01", CREATED_AT, CREATED_AT,
				List.of(new ApprovalWarningResponse(ApprovalWarningType.CONFLICT, "Phòng đã có lịch khác.")));
		when(registrationService.search(managerEmail, LoaiPhieu.HOC_TAP, null, "P0601", date, "SV001", 0, 20))
				.thenReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/registrations").with(user(managerEmail).roles("CBQL")).param("type", "HOC_TAP")
				.param("roomId", "P0601").param("date", date.toString()).param("creator", "SV001"))
				.andExpect(status().isOk()).andExpect(view().name("registration/list"))
				.andExpect(content().string(Matchers.containsString("Hàng đợi phiếu chờ duyệt")))
				.andExpect(content().string(Matchers.containsString("type=\"date\"")))
				.andExpect(content().string(Matchers.containsString("Phòng đã có lịch khác.")));
	}

	@Test
	void editRegistrationGroupsConsecutivePeriodsIntoOneRange() throws Exception {
		List<RegistrationScheduleResponse> schedules = List.of(
				new RegistrationScheduleResponse(2, "Thứ 2", 1, "Tiết 1", LocalTime.of(7, 0), LocalTime.of(7, 50)),
				new RegistrationScheduleResponse(2, "Thứ 2", 2, "Tiết 2", LocalTime.of(7, 50), LocalTime.of(8, 40)),
				new RegistrationScheduleResponse(2, "Thứ 2", 3, "Tiết 3", LocalTime.of(8, 40), LocalTime.of(9, 30)));
		when(registrationService.get(USER_EMAIL, REGISTRATION_ID)).thenReturn(response(schedules));

		var result = mockMvc
				.perform(get("/registrations/{id}/edit", REGISTRATION_ID).with(user(USER_EMAIL).roles("SV")))
				.andExpect(status().isOk()).andExpect(view().name("registration/form")).andReturn();

		RegistrationForms.RegistrationForm form = (RegistrationForms.RegistrationForm) result.getModelAndView()
				.getModel().get("registrationForm");
		assertThat(form.getSchedules()).singleElement().satisfies(range -> {
			assertThat(range.getDayOfWeek()).isEqualTo(2);
			assertThat(range.getStartPeriodId()).isEqualTo(1);
			assertThat(range.getEndPeriodId()).isEqualTo(3);
		});
	}

	@Test
	void registrationDetailRendersAggregateAndCancelRequiresCsrf() throws Exception {
		when(registrationService.get(LECTURER_EMAIL, REGISTRATION_ID)).thenReturn(response());

		mockMvc.perform(get("/registrations/{id}", REGISTRATION_ID).with(user(LECTURER_EMAIL).roles("GV")))
				.andExpect(status().isOk()).andExpect(view().name("registration/detail"))
				.andExpect(content().string(Matchers.containsString(REGISTRATION_ID)))
				.andExpect(content().string(Matchers.containsString("Khoảng tiết")))
				.andExpect(content().string(Matchers.containsString("Số tiết")))
				.andExpect(content().string(Matchers.containsString("INT1234")))
				.andExpect(content().string(Matchers.containsString("TB0001")))
				.andExpect(content().string(Matchers.containsString("GV001")))
				.andExpect(content().string(Matchers.containsString("name=\"_csrf\"")));

		mockMvc.perform(post("/registrations/{id}/cancel", REGISTRATION_ID).with(user(LECTURER_EMAIL).roles("GV"))
				.param("reason", "Không còn nhu cầu").param("version", "0")).andExpect(status().isForbidden());
		verify(registrationService, never()).cancel(org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
	}

	@Test
	void managerDetailRendersDecisionFormsAndDecisionPostsAreProtected() throws Exception {
		String managerEmail = "cb001@lab.local";
		when(registrationService.get(managerEmail, REGISTRATION_ID)).thenReturn(response());
		when(approvalService.preview(managerEmail, REGISTRATION_ID))
				.thenReturn(new ApprovalPreviewResponse(true, List.of()));

		mockMvc.perform(get("/registrations/{id}", REGISTRATION_ID).with(user(managerEmail).roles("CBQL")))
				.andExpect(status().isOk()).andExpect(view().name("registration/detail"))
				.andExpect(content().string(Matchers.containsString("Phê duyệt phiếu")))
				.andExpect(content().string(Matchers.containsString("Từ chối phiếu")))
				.andExpect(content().string(Matchers.containsString("name=\"deviceIds\"")));

		mockMvc.perform(post("/registrations/{id}/approve", REGISTRATION_ID).with(user(LECTURER_EMAIL).roles("GV"))
				.param("version", "0")).andExpect(status().isForbidden());
		mockMvc.perform(post("/registrations/{id}/approve", REGISTRATION_ID).with(user(managerEmail).roles("CBQL"))
				.param("version", "0")).andExpect(status().isForbidden());
		verify(approvalService, never()).approve(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any());
	}

	private RegistrationResponse response() {
		return response(List.of(
				new RegistrationScheduleResponse(2, "Thứ 2", 1, "Tiết 1", LocalTime.of(7, 0), LocalTime.of(7, 50))));
	}

	private RegistrationResponse response(List<RegistrationScheduleResponse> schedules) {
		return new RegistrationResponse(REGISTRATION_ID, LoaiPhieu.GIANG_DAY, "Thực hành mạng", "P0601", "Phòng 6.1",
				10, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), PhieuDangKyTrangThai.CHO_DUYET, 0, "GV001",
				"Giảng viên 01", "INT1234", "01", null, null, schedules,
				List.of(new RegistrationDeviceResponse("TB0001", "Robot", "Robot", true, true, false)),
				List.of(new RegistrationHistoryResponse(HanhDongXuLyPhieu.HUY, "SV001", "Sinh viên 01", "Đổi kế hoạch",
						CREATED_AT)),
				CREATED_AT, CREATED_AT, true, true);
	}
}
