package com.example.labmanagement.reporting.repository;

public record DashboardCounts(long actualSessions, long completedSessions, long inProgressSessions, long absentSessions,
		long incidents, long activeMaintenances) {
}
