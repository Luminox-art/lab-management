package com.example.labmanagement.scheduling.web;

import com.example.labmanagement.common.response.ApiResponse;
import com.example.labmanagement.scheduling.application.AvailabilityResponse;
import com.example.labmanagement.scheduling.application.RoomCalendarResponse;
import com.example.labmanagement.scheduling.application.SchedulingService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** API-15/16, FR-08/09, UC-06/07. */
@Validated
@RestController
@RequestMapping("/api/v1")
public class SchedulingRestController {

	private final SchedulingService schedulingService;

	public SchedulingRestController(SchedulingService schedulingService) {
		this.schedulingService = schedulingService;
	}

	@GetMapping("/availability")
	ApiResponse<AvailabilityResponse> availability(@RequestParam(required = false) String roomId,
			@RequestParam(required = false) List<String> deviceIds,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam @Min(2) @Max(8) int dayOfWeek, @RequestParam @Positive int periodId) {
		return ApiResponse.of(schedulingService.checkAvailability(roomId, deviceIds, from, to, dayOfWeek, periodId));
	}

	@GetMapping("/rooms/{id}/calendar")
	ApiResponse<RoomCalendarResponse> roomCalendar(@PathVariable String id,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return ApiResponse.of(schedulingService.roomCalendar(id, from, to));
	}
}
