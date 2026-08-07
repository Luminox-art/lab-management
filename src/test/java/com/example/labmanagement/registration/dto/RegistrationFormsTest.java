package com.example.labmanagement.registration.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class RegistrationFormsTest {

	@Test
	void scheduleRangeExpandsToIndividualPeriodRequests() {
		RegistrationForms.ScheduleForm range = new RegistrationForms.ScheduleForm(2, 1, 3);

		assertThat(range.toRequests()).extracting(item -> item.dayOfWeek() + "|" + item.periodId())
				.containsExactly("2|1", "2|2", "2|3");
	}

	@Test
	void consecutivePeriodsAreGroupedWhileGapsRemainSeparate() {
		List<RegistrationScheduleResponse> schedules = List.of(schedule(2, 1, 7, 0, 7, 50),
				schedule(2, 2, 7, 50, 8, 40), schedule(2, 3, 8, 40, 9, 30), schedule(2, 5, 10, 20, 11, 10),
				schedule(3, 1, 7, 0, 7, 50));

		assertThat(RegistrationForms.scheduleRanges(schedules))
				.extracting(range -> range.dayOfWeek() + "|" + range.startPeriodId() + "|" + range.endPeriodId())
				.containsExactly("2|1|3", "2|5|5", "3|1|1");
	}

	private RegistrationScheduleResponse schedule(int day, int period, int startHour, int startMinute, int endHour,
			int endMinute) {
		return new RegistrationScheduleResponse(day, "Thứ " + day, period, "Tiết " + period,
				LocalTime.of(startHour, startMinute), LocalTime.of(endHour, endMinute));
	}
}
