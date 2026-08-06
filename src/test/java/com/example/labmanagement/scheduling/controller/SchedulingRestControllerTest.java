package com.example.labmanagement.scheduling.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.labmanagement.catalog.domain.LoaiTaiNguyen;
import com.example.labmanagement.common.error.GlobalExceptionHandler;
import com.example.labmanagement.scheduling.dto.AvailabilityConflictResponse;
import com.example.labmanagement.scheduling.dto.AvailabilityConflictType;
import com.example.labmanagement.scheduling.dto.AvailabilityResponse;
import com.example.labmanagement.scheduling.dto.CalendarEventResponse;
import com.example.labmanagement.scheduling.dto.CalendarEventType;
import com.example.labmanagement.scheduling.dto.PeriodResponse;
import com.example.labmanagement.scheduling.dto.RoomCalendarResponse;
import com.example.labmanagement.scheduling.service.SchedulingService;
import com.example.labmanagement.security.SecurityConfiguration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** HTTP and privacy coverage for API-15/16. */
@WebMvcTest
@ContextConfiguration(classes = {SchedulingRestController.class, SecurityConfiguration.class,
		GlobalExceptionHandler.class})
class SchedulingRestControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SchedulingService schedulingService;

	@Test
	void availabilityRequiresAuthenticationAndReturnsConflictWithoutPersonalData() throws Exception {
		LocalDate from = LocalDate.of(2026, 9, 1);
		LocalDate to = LocalDate.of(2026, 9, 30);
		PeriodResponse period = new PeriodResponse(1, "Tiết 1", LocalTime.of(7, 0), LocalTime.of(7, 50));
		AvailabilityConflictResponse conflict = new AvailabilityConflictResponse(
				AvailabilityConflictType.ROOM_REGISTRATION, LoaiTaiNguyen.PHONG, "P0601", "Phòng 6.1",
				LocalDate.of(2026, 9, 7), 2, 1, "Tiết 1", "Phòng đã có lịch đã duyệt trong tiết này.");
		when(schedulingService.checkAvailability("P0601", List.of("TB0001"), from, to, 2, 1))
				.thenReturn(new AvailabilityResponse(false, from, to, 2, "Thứ 2", period,
						List.of(LocalDate.of(2026, 9, 7)), List.of(conflict)));

		mockMvc.perform(get("/api/v1/availability").param("roomId", "P0601").param("deviceIds", "TB0001")
				.param("from", "2026-09-01").param("to", "2026-09-30").param("dayOfWeek", "2").param("periodId", "1"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/v1/availability").with(user("student").roles("SV")).param("roomId", "P0601")
				.param("deviceIds", "TB0001").param("from", "2026-09-01").param("to", "2026-09-30")
				.param("dayOfWeek", "2").param("periodId", "1")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.available").value(false))
				.andExpect(jsonPath("$.data.conflicts[0].type").value("ROOM_REGISTRATION"))
				.andExpect(content().string(Matchers.not(Matchers.containsString("registrationId"))))
				.andExpect(content().string(Matchers.not(Matchers.containsString("creator"))))
				.andExpect(content().string(Matchers.not(Matchers.containsString("purpose"))))
				.andExpect(content().string(Matchers.not(Matchers.containsString("email"))));
	}

	@Test
	void invalidOrMissingAvailabilityParametersReturnStandardValidationError() throws Exception {
		mockMvc.perform(get("/api/v1/availability").with(user("student").roles("SV")).param("roomId", "P0601")
				.param("from", "2026-09-01").param("to", "2026-09-30").param("dayOfWeek", "9").param("periodId", "1"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		mockMvc.perform(get("/api/v1/availability").with(user("student").roles("SV")).param("roomId", "P0601")
				.param("from", "2026-09-01").param("dayOfWeek", "2").param("periodId", "1"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void roomCalendarDistinguishesApprovedInUseAndBlockedEvents() throws Exception {
		LocalDate from = LocalDate.of(2026, 9, 7);
		LocalDate to = LocalDate.of(2026, 9, 13);
		CalendarEventResponse approved = new CalendarEventResponse(CalendarEventType.APPROVED_REGISTRATION, from, 2, 1,
				"Tiết 1", LocalTime.of(7, 0), LocalTime.of(7, 50), false, "Lịch đã duyệt", "Đã xếp lịch");
		CalendarEventResponse blocked = new CalendarEventResponse(CalendarEventType.BLOCKED_SCHEDULE, from, 2, null,
				null, null, null, true, "Lịch chặn", "Bảo trì");
		when(schedulingService.roomCalendar("P0601", from, to)).thenReturn(
				new RoomCalendarResponse("P0601", "Phòng 6.1", from, to, List.of(), List.of(approved, blocked)));

		mockMvc.perform(get("/api/v1/rooms/P0601/calendar").with(user("lecturer").roles("GV"))
				.param("from", "2026-09-07").param("to", "2026-09-13")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.events[0].type").value("APPROVED_REGISTRATION"))
				.andExpect(jsonPath("$.data.events[1].type").value("BLOCKED_SCHEDULE"))
				.andExpect(jsonPath("$.data.events[1].allDay").value(true));
	}
}
