package com.example.labmanagement.scheduling.web;

import com.example.labmanagement.catalog.application.CatalogService;
import com.example.labmanagement.common.error.ApiException;
import com.example.labmanagement.scheduling.application.AvailabilityResponse;
import com.example.labmanagement.scheduling.application.CalendarEventResponse;
import com.example.labmanagement.scheduling.application.RoomCalendarResponse;
import com.example.labmanagement.scheduling.application.SchedulingService;
import com.example.labmanagement.scheduling.domain.ScheduleDateCalculator;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/schedule")
public class SchedulingWebController {

	private static final DateTimeFormatter DATE_LABEL = DateTimeFormatter.ofPattern("dd/MM/yyyy",
			Locale.forLanguageTag("vi-VN"));

	private final SchedulingService schedulingService;
	private final CatalogService catalogService;

	public SchedulingWebController(SchedulingService schedulingService, CatalogService catalogService) {
		this.schedulingService = schedulingService;
		this.catalogService = catalogService;
	}

	@GetMapping("/availability")
	String availability(@RequestParam(required = false) String roomId,
			@RequestParam(required = false) List<String> deviceIds, @RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to, @RequestParam(required = false) Integer dayOfWeek,
			@RequestParam(required = false) Integer periodId, @RequestParam(defaultValue = "false") boolean check,
			Model model) {
		LocalDate normalizedFrom = from == null ? LocalDate.now() : from;
		LocalDate normalizedTo = to == null ? normalizedFrom.plusDays(30) : to;
		int normalizedDay = dayOfWeek == null ? ScheduleDateCalculator.toSystemDay(normalizedFrom) : dayOfWeek;
		int normalizedPeriod = periodId == null ? 1 : periodId;
		List<String> normalizedDeviceIds = deviceIds == null ? List.of() : deviceIds;

		if (check) {
			try {
				AvailabilityResponse result = schedulingService.checkAvailability(roomId, normalizedDeviceIds,
						normalizedFrom, normalizedTo, normalizedDay, normalizedPeriod);
				model.addAttribute("result", result);
			} catch (ApiException exception) {
				model.addAttribute("error", exception.getMessage());
			}
		}

		model.addAttribute("rooms", catalogService.selectableRooms());
		model.addAttribute("devices", catalogService.selectableDevices());
		model.addAttribute("periods", schedulingService.periods());
		model.addAttribute("dayOptions", dayOptions());
		model.addAttribute("roomId", roomId);
		model.addAttribute("deviceIds", normalizedDeviceIds);
		model.addAttribute("from", normalizedFrom);
		model.addAttribute("to", normalizedTo);
		model.addAttribute("dayOfWeek", normalizedDay);
		model.addAttribute("periodId", normalizedPeriod);
		model.addAttribute("checked", check);
		return "schedule/availability";
	}

	@GetMapping("/calendar")
	String calendar(@RequestParam(required = false) String roomId, @RequestParam(required = false) LocalDate date,
			@RequestParam(defaultValue = "week") String view, Authentication authentication, Model model) {
		LocalDate selectedDate = date == null ? LocalDate.now() : date;
		String normalizedView = "day".equalsIgnoreCase(view) ? "day" : "week";
		LocalDate from = "day".equals(normalizedView) ? selectedDate : selectedDate.with(DayOfWeek.MONDAY);
		LocalDate to = "day".equals(normalizedView) ? selectedDate : from.plusDays(6);
		RoomCalendarResponse result = null;
		if (roomId != null && !roomId.isBlank()) {
			try {
				result = schedulingService.roomCalendar(roomId, from, to);
				model.addAttribute("result", result);
				model.addAttribute("calendarDays", calendarDays(result));
			} catch (ApiException exception) {
				model.addAttribute("error", exception.getMessage());
			}
		}

		int step = "day".equals(normalizedView) ? 1 : 7;
		model.addAttribute("rooms", catalogService.roomsForFilter());
		model.addAttribute("roomId", roomId);
		model.addAttribute("selectedDate", selectedDate);
		model.addAttribute("view", normalizedView);
		model.addAttribute("from", from);
		model.addAttribute("to", to);
		model.addAttribute("previousDate", selectedDate.minusDays(step));
		model.addAttribute("nextDate", selectedDate.plusDays(step));
		model.addAttribute("today", LocalDate.now());
		model.addAttribute("manager", authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_CBQL".equals(authority.getAuthority())));
		return "schedule/calendar";
	}

	private List<DayOption> dayOptions() {
		List<DayOption> options = new ArrayList<>();
		for (int day = ScheduleDateCalculator.MONDAY; day <= ScheduleDateCalculator.SUNDAY; day++) {
			options.add(new DayOption(day, ScheduleDateCalculator.systemDayLabel(day)));
		}
		return List.copyOf(options);
	}

	private List<CalendarDayView> calendarDays(RoomCalendarResponse calendar) {
		Map<LocalDate, List<CalendarEventResponse>> eventsByDate = new LinkedHashMap<>();
		for (LocalDate date : ScheduleDateCalculator.datesBetween(calendar.from(), calendar.to())) {
			eventsByDate.put(date, new ArrayList<>());
		}
		for (CalendarEventResponse event : calendar.events()) {
			eventsByDate.computeIfAbsent(event.date(), ignored -> new ArrayList<>()).add(event);
		}
		return eventsByDate.entrySet().stream()
				.map(entry -> new CalendarDayView(entry.getKey(),
						ScheduleDateCalculator.systemDayLabel(ScheduleDateCalculator.toSystemDay(entry.getKey())) + ", "
								+ DATE_LABEL.format(entry.getKey()),
						List.copyOf(entry.getValue())))
				.toList();
	}

	public record DayOption(int value, String label) {
	}

	public record CalendarDayView(LocalDate date, String label, List<CalendarEventResponse> events) {
	}
}
