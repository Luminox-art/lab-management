package com.example.labmanagement.registration.dto;

public record RegistrationDeviceResponse(String id, String name, String typeName, boolean instructorRequired,
		boolean mobile, boolean allocated) {
}
