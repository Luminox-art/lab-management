package com.example.labmanagement.scheduling.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.labmanagement.catalog.domain.LoaiTaiNguyen;
import com.example.labmanagement.catalog.domain.PhongTrangThai;
import com.example.labmanagement.catalog.domain.ThietBiTrangThai;
import com.example.labmanagement.catalog.dto.DeviceResponse;
import com.example.labmanagement.catalog.dto.RoomResponse;
import com.example.labmanagement.catalog.service.CatalogService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SchedulingWebController.class)
@Import(SecurityConfiguration.class)
class SchedulingWebControllerTest {

	private static final LocalDate MONDAY = LocalDate.of(2026, 9, 7);
	private static final PeriodResponse PERIOD = new PeriodResponse(1, "Tiết 1", LocalTime.of(7, 0),
			LocalTime.of(7, 50));

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SchedulingService schedulingService;

	@MockitoBean
	private CatalogService catalogService;

	@BeforeEach
	void setUp() {
		RoomResponse room = new RoomResponse("P0601", "Phòng 6.1", "NP01", "Nhóm phòng máy", "Tầng 6", 40,
				PhongTrangThai.SAN_SANG, 0);
		DeviceResponse device = new DeviceResponse("TB0001", "Máy chiếu", "MC", "Máy chiếu", false, true, "SERIAL-1",
				"Model A", "P0601", "Phòng 6.1", ThietBiTrangThai.SAN_SANG, 0);
		when(catalogService.selectableRooms()).thenReturn(List.of(room));
		when(catalogService.selectableDevices()).thenReturn(List.of(device));
		when(catalogService.roomsForFilter()).thenReturn(List.of(room));
		when(schedulingService.periods()).thenReturn(List.of(PERIOD));
	}

	@Test
	void pagesRequireAuthenticationAndInitialAvailabilityFormIsAccessible() throws Exception {
		mockMvc.perform(get("/schedule/availability")).andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("**/login"));

		mockMvc.perform(get("/schedule/availability").with(user("student").roles("SV"))).andExpect(status().isOk())
				.andExpect(view().name("scheduling/availability"))
				.andExpect(content().string(Matchers.containsString("Điều kiện cần kiểm tra")))
				.andExpect(content().string(Matchers.containsString("Giữ Ctrl hoặc Command")));
	}

	@Test
	void submittedAvailabilityRendersConflictsWithoutPersonalRegistrationData() throws Exception {
		AvailabilityConflictResponse conflict = new AvailabilityConflictResponse(
				AvailabilityConflictType.ROOM_REGISTRATION, LoaiTaiNguyen.PHONG, "P0601", "Phòng 6.1", MONDAY, 2, 1,
				"Tiết 1", "Phòng đã có lịch đã duyệt trong tiết này.");
		when(schedulingService.checkAvailability("P0601", List.of("TB0001"), MONDAY, MONDAY, 2, 1))
				.thenReturn(new AvailabilityResponse(false, MONDAY, MONDAY, 2, "Thứ 2", PERIOD, List.of(MONDAY),
						List.of(conflict)));

		mockMvc.perform(get("/schedule/availability").with(user("lecturer").roles("GV")).param("check", "true")
				.param("roomId", "P0601").param("deviceIds", "TB0001").param("from", "2026-09-07")
				.param("to", "2026-09-07").param("dayOfWeek", "2").param("periodId", "1")).andExpect(status().isOk())
				.andExpect(content().string(Matchers.containsString("Không khả dụng")))
				.andExpect(content().string(Matchers.containsString("Trùng lịch phòng")))
				.andExpect(content().string(Matchers.not(Matchers.containsString("registrationId"))))
				.andExpect(content().string(Matchers.not(Matchers.containsString("Người tạo"))))
				.andExpect(content().string(Matchers.not(Matchers.containsString("Mục đích"))));
	}

	@Test
	void weeklyCalendarRendersAllPublicEventStatesAndNavigation() throws Exception {
		LocalDate sunday = MONDAY.plusDays(6);
		List<CalendarEventResponse> events = List.of(
				new CalendarEventResponse(CalendarEventType.APPROVED_REGISTRATION, MONDAY, 2, 1, "Tiết 1",
						LocalTime.of(7, 0), LocalTime.of(7, 50), false, "Lịch đã duyệt", "Đã xếp lịch"),
				new CalendarEventResponse(CalendarEventType.APPROVED_REGISTRATION, MONDAY, 2, 2, "Tiết 2",
						LocalTime.of(7, 50), LocalTime.of(8, 40), false, "Lịch đã duyệt", "Đã xếp lịch"),
				new CalendarEventResponse(CalendarEventType.APPROVED_REGISTRATION, MONDAY, 2, 3, "Tiết 3",
						LocalTime.of(8, 40), LocalTime.of(9, 30), false, "Lịch đã duyệt", "Đã xếp lịch"),
				new CalendarEventResponse(CalendarEventType.IN_USE_REGISTRATION, MONDAY, 2, 1, "Tiết 1",
						LocalTime.of(7, 0), LocalTime.of(7, 50), false, "Lịch đang sử dụng", "Đang sử dụng"),
				new CalendarEventResponse(CalendarEventType.BLOCKED_SCHEDULE, MONDAY, 2, null, null, null, null, true,
						"Lịch chặn", "Bảo trì"));
		when(schedulingService.roomCalendar("P0601", MONDAY, sunday))
				.thenReturn(new RoomCalendarResponse("P0601", "Phòng 6.1", MONDAY, sunday, List.of(PERIOD), events));

		mockMvc.perform(get("/schedule/calendar").with(user("student").roles("SV")).param("roomId", "P0601")
				.param("date", "2026-09-07").param("view", "week")).andExpect(status().isOk())
				.andExpect(view().name("scheduling/calendar"))
				.andExpect(content().string(Matchers.containsString("Đã duyệt")))
				.andExpect(content().string(Matchers.containsString("Đang sử dụng")))
				.andExpect(content().string(Matchers.containsString("Lịch chặn")))
				.andExpect(content().string(Matchers.containsString("Tiết 1–3 . 07:00–09:30")))
				.andExpect(content().string(Matchers.containsString("calendar-scroll")))
				.andExpect(content().string(Matchers.containsString("Hôm nay")))
				.andExpect(content().string(Matchers.containsString("Chủ nhật, 13/09/2026")));
	}
}
