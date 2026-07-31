package com.example.labmanagement.catalog.application;

public record DeviceTypeResponse(String id, String name, boolean instructorRequired, boolean mobile,
		String description) {
}
