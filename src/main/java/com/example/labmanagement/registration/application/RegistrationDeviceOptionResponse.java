package com.example.labmanagement.registration.application;

public record RegistrationDeviceOptionResponse(String id, String name, String typeName, boolean instructorRequired,
		boolean mobile, String roomId) {
}
