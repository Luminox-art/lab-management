package com.example.labmanagement.scheduling.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class ScheduleDateCalculator {

	public static final int MONDAY = 2;
	public static final int SUNDAY = 8;

	private ScheduleDateCalculator() {
	}

	public static List<LocalDate> datesForSystemDay(LocalDate from, LocalDate to, int systemDayOfWeek) {
		validateRange(from, to);
		DayOfWeek targetDay = toJavaDay(systemDayOfWeek);
		int daysUntilTarget = Math.floorMod(targetDay.getValue() - from.getDayOfWeek().getValue(), 7);
		List<LocalDate> dates = new ArrayList<>();
		for (LocalDate date = from.plusDays(daysUntilTarget); !date.isAfter(to); date = date.plusWeeks(1)) {
			dates.add(date);
		}
		return List.copyOf(dates);
	}

	public static List<LocalDate> datesBetween(LocalDate from, LocalDate to) {
		validateRange(from, to);
		List<LocalDate> dates = new ArrayList<>();
		for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
			dates.add(date);
		}
		return List.copyOf(dates);
	}

	public static boolean overlaps(LocalDate firstStart, LocalDate firstEnd, LocalDate secondStart,
			LocalDate secondEnd) {
		return !firstEnd.isBefore(secondStart) && !secondEnd.isBefore(firstStart);
	}

	public static int toSystemDay(LocalDate date) {
		int javaDay = date.getDayOfWeek().getValue();
		return javaDay == DayOfWeek.SUNDAY.getValue() ? SUNDAY : javaDay + 1;
	}

	public static String systemDayLabel(int systemDayOfWeek) {
		validateSystemDay(systemDayOfWeek);
		return systemDayOfWeek == SUNDAY ? "Chủ nhật" : "Thứ " + systemDayOfWeek;
	}

	public static LocalDate later(LocalDate first, LocalDate second) {
		return first.isAfter(second) ? first : second;
	}

	public static LocalDate earlier(LocalDate first, LocalDate second) {
		return first.isBefore(second) ? first : second;
	}

	public static void validateRange(LocalDate from, LocalDate to) {
		if (from == null || to == null || from.isAfter(to)) {
			throw new IllegalArgumentException("Ngày bắt đầu phải không sau ngày kết thúc.");
		}
	}

	public static void validateSystemDay(int systemDayOfWeek) {
		if (systemDayOfWeek < MONDAY || systemDayOfWeek > SUNDAY) {
			throw new IllegalArgumentException("Thứ phải nằm trong khoảng 2 đến 8.");
		}
	}

	private static DayOfWeek toJavaDay(int systemDayOfWeek) {
		validateSystemDay(systemDayOfWeek);
		return systemDayOfWeek == SUNDAY ? DayOfWeek.SUNDAY : DayOfWeek.of(systemDayOfWeek - 1);
	}
}
