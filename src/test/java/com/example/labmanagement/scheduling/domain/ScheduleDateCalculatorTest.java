package com.example.labmanagement.scheduling.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Boundary coverage for TC-APR-01..06 and TC-REG-01..03. */
class ScheduleDateCalculatorTest {

	@Test
	void includesLeapDayWhenItMatchesRequestedSystemDay() {
		assertThat(ScheduleDateCalculator.datesForSystemDay(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 3, 2), 5))
				.contains(LocalDate.of(2024, 2, 29));
	}

	@Test
	void crossesMonthBoundaryAndIncludesBothRangeEdges() {
		assertThat(ScheduleDateCalculator.datesForSystemDay(LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 14), 2))
				.containsExactly(LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 14));
	}

	@Test
	void mapsSundayToEightAndMondayToTwo() {
		assertThat(ScheduleDateCalculator.toSystemDay(LocalDate.of(2026, 8, 2))).isEqualTo(8);
		assertThat(ScheduleDateCalculator.toSystemDay(LocalDate.of(2026, 8, 3))).isEqualTo(2);
		assertThat(ScheduleDateCalculator.systemDayLabel(8)).isEqualTo("Chủ nhật");
	}

	@Test
	void overlapIsInclusiveAtBothBoundaries() {
		assertThat(ScheduleDateCalculator.overlaps(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 7),
				LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 10))).isTrue();
		assertThat(ScheduleDateCalculator.overlaps(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 6),
				LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 10))).isFalse();
	}

	@Test
	void rejectsInvalidRangeAndSystemDay() {
		assertThatThrownBy(
				() -> ScheduleDateCalculator.datesForSystemDay(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 1), 2))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(
				() -> ScheduleDateCalculator.datesForSystemDay(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2), 1))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
