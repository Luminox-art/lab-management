package com.example.labmanagement.catalog.dto;

public record DeviceTypeResponse(String id, String name, boolean instructorRequired, boolean mobile,
		String description) {
}
