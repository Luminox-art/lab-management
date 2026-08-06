package com.example.labmanagement.incident.dto;

import com.example.labmanagement.catalog.domain.LoaiTaiNguyen;

public record IncidentResourceOptionResponse(String id, LoaiTaiNguyen type, String referenceId, String name) {
}
