package com.example.labmanagement.catalog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.labmanagement.catalog.domain.PhongTrangThai;
import com.example.labmanagement.catalog.domain.ThietBiTrangThai;
import com.example.labmanagement.catalog.dto.DeviceResponse;
import com.example.labmanagement.catalog.dto.DeviceUpdateRequest;
import com.example.labmanagement.catalog.dto.RoomCreateRequest;
import com.example.labmanagement.catalog.dto.RoomResponse;
import com.example.labmanagement.catalog.service.CatalogService;
import com.example.labmanagement.common.error.GlobalExceptionHandler;
import com.example.labmanagement.security.SecurityConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Security and HTTP coverage for TC-CAT-01..14, API-09..14. */
@WebMvcTest
@ContextConfiguration(classes = {RoomRestController.class, DeviceRestController.class, SecurityConfiguration.class,
		GlobalExceptionHandler.class})
class CatalogRestControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CatalogService catalogService;

	@Test
	void authenticatedUserCanSearchRoomsWithPageMetadata() throws Exception {
		RoomResponse room = new RoomResponse("P01", "Phòng 01", "NP01", "Tầng 1", "Nhà A", 30, PhongTrangThai.SAN_SANG,
				0);
		when(catalogService.searchRooms("NP01", PhongTrangThai.SAN_SANG, "P01", 0, 20, "name,asc"))
				.thenReturn(new PageImpl<>(List.of(room), PageRequest.of(0, 20), 1));
		when(catalogService.normalizeRoomSort("name,asc")).thenReturn("name,asc");

		mockMvc.perform(get("/api/v1/rooms").with(user("student").roles("SV")).param("group", "NP01")
				.param("status", "SAN_SANG").param("keyword", "P01").param("sort", "name,asc"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value("P01"))
				.andExpect(jsonPath("$.meta.totalElements").value(1))
				.andExpect(jsonPath("$.meta.sort").value("name,asc"));
	}

	@Test
	void onlyManagerWithCsrfCanCreateRoom() throws Exception {
		String body = """
				{"id":"P-NEW","name":"Phòng mới","groupId":"NP01","location":"Tầng 2",
				 "capacity":24,"status":"SAN_SANG"}
				""";
		RoomResponse response = new RoomResponse("P-NEW", "Phòng mới", "NP01", "Nhóm 01", "Tầng 2", 24,
				PhongTrangThai.SAN_SANG, 0);
		when(catalogService.createRoom(any(RoomCreateRequest.class))).thenReturn(response);

		mockMvc.perform(post("/api/v1/rooms").with(user("lecturer").roles("GV")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/rooms").with(user("manager").roles("CBQL"))
				.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/rooms").with(user("manager").roles("CBQL")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.id").value("P-NEW"));
		verify(catalogService).createRoom(any(RoomCreateRequest.class));
	}

	@Test
	void roomCapacityMustBePositive() throws Exception {
		String body = """
				{"id":"P-BAD","name":"Phòng lỗi","groupId":"NP01","location":"Tầng 2",
				 "capacity":0,"status":"SAN_SANG"}
				""";

		mockMvc.perform(post("/api/v1/rooms").with(user("manager").roles("CBQL")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("capacity"));
	}

	@Test
	void authenticatedUserCanSearchDevicesAndOnlyManagerCanUpdate() throws Exception {
		DeviceResponse device = new DeviceResponse("TB01", "Máy tính", "PC", "Máy tính", false, false, "SERIAL-01",
				"M1", "P01", "Phòng 01", ThietBiTrangThai.SAN_SANG, 2);
		when(catalogService.searchDevices(null, null, null, null, 0, 20, "id,asc"))
				.thenReturn(new PageImpl<>(List.of(device), PageRequest.of(0, 20), 1));
		when(catalogService.normalizeDeviceSort("id,asc")).thenReturn("id,asc");
		when(catalogService.updateDevice(eq("TB01"), any(DeviceUpdateRequest.class))).thenReturn(device);

		mockMvc.perform(get("/api/v1/devices").with(user("student").roles("SV"))).andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].serialNumber").value("SERIAL-01"));

		String body = """
				{"name":"Máy tính","typeId":"PC","serialNumber":"SERIAL-01","model":"M1",
				 "roomId":"P01","status":"SAN_SANG","version":2}
				""";
		mockMvc.perform(put("/api/v1/devices/TB01").with(user("student").roles("SV")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
		mockMvc.perform(put("/api/v1/devices/TB01").with(user("manager").roles("CBQL")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.version").value(2));
	}
}
