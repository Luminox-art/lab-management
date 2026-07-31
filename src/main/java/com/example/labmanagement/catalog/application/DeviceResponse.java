package com.example.labmanagement.catalog.application;

import com.example.labmanagement.catalog.domain.ThietBiTrangThai;

public record DeviceResponse(String id, String name, String typeId, String typeName, boolean instructorRequired,
		boolean mobile, String serialNumber, String model, String roomId, String roomName, ThietBiTrangThai status,
		long version) {
}
