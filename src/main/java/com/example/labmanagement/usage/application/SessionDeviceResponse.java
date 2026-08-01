package com.example.labmanagement.usage.application;

public record SessionDeviceResponse(String id, String name, String typeName, String receivedCondition,
		String returnedCondition, String note) {
}
