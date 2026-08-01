package com.example.labmanagement.reporting.persistence;

public record DashboardCounts(long actualSessions, long completedSessions, long inProgressSessions, long absentSessions,
		long incidents, long activeMaintenances) {
}
