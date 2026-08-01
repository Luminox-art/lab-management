package com.example.labmanagement.incident.application;

import com.example.labmanagement.catalog.domain.LoaiTaiNguyen;

public record IncidentResourceOptionResponse(String id, LoaiTaiNguyen type, String referenceId, String name) {
}
