package com.example.labmanagement.maintenance.dto;

import com.example.labmanagement.catalog.domain.LoaiTaiNguyen;

public record MaintenanceResourceOptionResponse(String id, LoaiTaiNguyen type, String referenceId, String name) {
}
