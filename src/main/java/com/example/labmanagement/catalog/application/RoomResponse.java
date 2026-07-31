package com.example.labmanagement.catalog.application;

import com.example.labmanagement.catalog.domain.PhongTrangThai;

public record RoomResponse(String id, String name, String groupId, String groupName, String location, int capacity,
		PhongTrangThai status, long version) {
}
