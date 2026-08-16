package com.example.labmanagement.registration.dto;

public record RegistrationDeviceOptionResponse(String id, String name, String typeName, boolean instructorRequired,
		boolean mobile, String managementRoomId, String currentUsageRoomId) {
}
