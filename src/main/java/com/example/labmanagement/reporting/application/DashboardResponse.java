package com.example.labmanagement.reporting.application;

import com.example.labmanagement.reporting.domain.DashboardGroup;
import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(LocalDate from, LocalDate to, DashboardGroup groupBy, DashboardSummaryResponse summary,
		List<DashboardFrequencyResponse> frequencies, List<DashboardSeverityResponse> incidentSeverities,
		List<DashboardMaintenanceResponse> activeMaintenances) {
}
