package com.example.labmanagement.catalog.dto;

import com.example.labmanagement.catalog.domain.PhongTrangThai;
import com.example.labmanagement.catalog.domain.ThietBiTrangThai;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class CatalogForms {

	private CatalogForms() {
	}

	public static final class RoomForm {

		@NotBlank
		@Size(max = 50)
		private String id;
		@NotBlank
		@Size(max = 150)
		private String name;
		@NotBlank
		@Size(max = 50)
		private String groupId;
		@NotBlank
		@Size(max = 255)
		private String location;
		@NotNull
		@Positive
		private Integer capacity;
		@NotNull
		private PhongTrangThai status = PhongTrangThai.SAN_SANG;
		private long version;

		public static RoomForm from(RoomResponse room) {
			RoomForm form = new RoomForm();
			form.id = room.id();
			form.name = room.name();
			form.groupId = room.groupId();
			form.location = room.location();
			form.capacity = room.capacity();
			form.status = room.status();
			form.version = room.version();
			return form;
		}

		public RoomCreateRequest toCreateRequest() {
			return new RoomCreateRequest(id, name, groupId, location, capacity == null ? 0 : capacity, status);
		}

		public RoomUpdateRequest toUpdateRequest() {
			return new RoomUpdateRequest(name, groupId, location, capacity == null ? 0 : capacity, status, version);
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getGroupId() {
			return groupId;
		}

		public void setGroupId(String groupId) {
			this.groupId = groupId;
		}

		public String getLocation() {
			return location;
		}

		public void setLocation(String location) {
			this.location = location;
		}

		public Integer getCapacity() {
			return capacity;
		}

		public void setCapacity(Integer capacity) {
			this.capacity = capacity;
		}

		public PhongTrangThai getStatus() {
			return status;
		}

		public void setStatus(PhongTrangThai status) {
			this.status = status;
		}

		public long getVersion() {
			return version;
		}

		public void setVersion(long version) {
			this.version = version;
		}
	}

	public static final class DeviceForm {

		@NotBlank
		@Size(max = 50)
		private String id;
		@NotBlank
		@Size(max = 150)
		private String name;
		@NotBlank
		@Size(max = 50)
		private String typeId;
		@Size(max = 100)
		private String serialNumber;
		@Size(max = 100)
		private String model;
		@Size(max = 50)
		private String roomId;
		@NotNull
		private ThietBiTrangThai status = ThietBiTrangThai.SAN_SANG;
		private long version;

		public static DeviceForm from(DeviceResponse device) {
			DeviceForm form = new DeviceForm();
			form.id = device.id();
			form.name = device.name();
			form.typeId = device.typeId();
			form.serialNumber = device.serialNumber();
			form.model = device.model();
			form.roomId = device.roomId();
			form.status = device.status();
			form.version = device.version();
			return form;
		}

		public DeviceCreateRequest toCreateRequest() {
			return new DeviceCreateRequest(id, name, typeId, serialNumber, model, roomId, status);
		}

		public DeviceUpdateRequest toUpdateRequest() {
			return new DeviceUpdateRequest(name, typeId, serialNumber, model, roomId, status, version);
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getTypeId() {
			return typeId;
		}

		public void setTypeId(String typeId) {
			this.typeId = typeId;
		}

		public String getSerialNumber() {
			return serialNumber;
		}

		public void setSerialNumber(String serialNumber) {
			this.serialNumber = serialNumber;
		}

		public String getModel() {
			return model;
		}

		public void setModel(String model) {
			this.model = model;
		}

		public String getRoomId() {
			return roomId;
		}

		public void setRoomId(String roomId) {
			this.roomId = roomId;
		}

		public ThietBiTrangThai getStatus() {
			return status;
		}

		public void setStatus(ThietBiTrangThai status) {
			this.status = status;
		}

		public long getVersion() {
			return version;
		}

		public void setVersion(long version) {
			this.version = version;
		}
	}

	public static final class RoomGroupForm {

		private String originalId;
		@NotBlank
		@Size(max = 50)
		private String id;
		@NotBlank
		@Size(max = 150)
		private String name;
		@Size(max = 5000)
		private String description;

		public static RoomGroupForm from(RoomGroupResponse group) {
			RoomGroupForm form = new RoomGroupForm();
			form.originalId = group.id();
			form.id = group.id();
			form.name = group.name();
			form.description = group.description();
			return form;
		}

		public RoomGroupRequest toRequest() {
			return new RoomGroupRequest(id, name, description);
		}

		public String getOriginalId() {
			return originalId;
		}

		public void setOriginalId(String originalId) {
			this.originalId = originalId;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}

	public static final class DeviceTypeForm {

		private String originalId;
		@NotBlank
		@Size(max = 50)
		private String id;
		@NotBlank
		@Size(max = 150)
		private String name;
		private boolean instructorRequired;
		private boolean mobile;
		@Size(max = 5000)
		private String description;

		public static DeviceTypeForm from(DeviceTypeResponse type) {
			DeviceTypeForm form = new DeviceTypeForm();
			form.originalId = type.id();
			form.id = type.id();
			form.name = type.name();
			form.instructorRequired = type.instructorRequired();
			form.mobile = type.mobile();
			form.description = type.description();
			return form;
		}

		public DeviceTypeRequest toRequest() {
			return new DeviceTypeRequest(id, name, instructorRequired, mobile, description);
		}

		public String getOriginalId() {
			return originalId;
		}

		public void setOriginalId(String originalId) {
			this.originalId = originalId;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public boolean isInstructorRequired() {
			return instructorRequired;
		}

		public void setInstructorRequired(boolean instructorRequired) {
			this.instructorRequired = instructorRequired;
		}

		public boolean isMobile() {
			return mobile;
		}

		public void setMobile(boolean mobile) {
			this.mobile = mobile;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}
}
