package com.example.labmanagement.reporting.application;

import com.example.labmanagement.reporting.domain.DashboardGroup;

public record DashboardFrequencyResponse(DashboardGroup group, String id, String name, long count,
		int relativePercent) {
}
