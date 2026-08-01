package com.example.labmanagement.reporting.application;

import com.example.labmanagement.incident.domain.MucDoSuCo;

public record DashboardSeverityResponse(MucDoSuCo severity, long count, int percent) {
}
