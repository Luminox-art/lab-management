package com.example.labmanagement.reporting.application;

public record DashboardSummaryResponse(long actualSessions, long completedSessions, long inProgressSessions,
		double completionRate, long absentSessions, double absenceRate, long incidents, long activeMaintenances) {
}
