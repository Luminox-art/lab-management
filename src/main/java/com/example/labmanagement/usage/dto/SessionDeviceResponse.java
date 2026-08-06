package com.example.labmanagement.usage.dto;

public record SessionDeviceResponse(String id, String name, String typeName, String receivedCondition,
		String returnedCondition, String note) {
}
