package com.example.labmanagement.reporting.dto;

import com.example.labmanagement.reporting.domain.DashboardGroup;

public record DashboardFrequencyResponse(DashboardGroup group, String id, String name, long count,
		int relativePercent) {
}
