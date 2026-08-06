package com.example.labmanagement.catalog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.labmanagement.catalog.dto.DeviceTypeResponse;
import com.example.labmanagement.catalog.dto.RoomGroupResponse;
import com.example.labmanagement.catalog.service.CatalogService;
import com.example.labmanagement.security.SecurityConfiguration;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CatalogWebController.class)
@Import(SecurityConfiguration.class)
class CatalogWebControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CatalogService catalogService;

	@BeforeEach
	void setUp() {
		when(catalogService.searchRooms(null, null, null, 0, 20, "id,asc")).thenReturn(Page.empty());
		when(catalogService.searchDevices(null, null, null, null, 0, 20, "id,asc")).thenReturn(Page.empty());
		when(catalogService.normalizeRoomSort("id,asc")).thenReturn("id,asc");
		when(catalogService.normalizeDeviceSort("id,asc")).thenReturn("id,asc");
		when(catalogService.roomGroups()).thenReturn(List.of(new RoomGroupResponse("NP01", "Nhóm 01", null)));
		when(catalogService.deviceTypes())
				.thenReturn(List.of(new DeviceTypeResponse("PC", "Máy tính", false, false, null)));
		when(catalogService.selectableRooms()).thenReturn(List.of());
		when(catalogService.roomsForFilter()).thenReturn(List.of());
	}

	@Test
	void regularUserCanViewSearchPagesButCannotSeeManagementActions() throws Exception {
		mockMvc.perform(get("/catalog/rooms").with(user("student").roles("SV"))).andExpect(status().isOk())
				.andExpect(view().name("catalog/rooms")).andExpect(content().string(Matchers.containsString("Bộ lọc")))
				.andExpect(content().string(Matchers.not(Matchers.containsString("Thêm phòng"))));

		mockMvc.perform(get("/catalog/devices").with(user("lecturer").roles("GV"))).andExpect(status().isOk())
				.andExpect(view().name("catalog/devices"))
				.andExpect(content().string(Matchers.containsString("Mã, tên hoặc serial")))
				.andExpect(content().string(Matchers.not(Matchers.containsString("Thêm thiết bị"))));
	}

	@Test
	void onlyManagerCanOpenManagementForms() throws Exception {
		mockMvc.perform(get("/catalog/room-groups").with(user("lecturer").roles("GV")))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/catalog/room-groups").with(user("manager").roles("CBQL"))).andExpect(status().isOk())
				.andExpect(view().name("catalog/room-groups"))
				.andExpect(content().string(Matchers.containsString("Thêm nhóm phòng")));
	}

	@Test
	void managerFormsRenderWithAccessibleLabelsAndCatalogOptions() throws Exception {
		mockMvc.perform(get("/catalog/rooms/new").with(user("manager").roles("CBQL"))).andExpect(status().isOk())
				.andExpect(view().name("catalog/room-form"))
				.andExpect(content().string(Matchers.containsString("Sức chứa")));
		mockMvc.perform(get("/catalog/devices/new").with(user("manager").roles("CBQL"))).andExpect(status().isOk())
				.andExpect(view().name("catalog/device-form"))
				.andExpect(content().string(Matchers.containsString("Số serial")));
		mockMvc.perform(get("/catalog/device-types").with(user("manager").roles("CBQL"))).andExpect(status().isOk())
				.andExpect(view().name("catalog/device-types"))
				.andExpect(content().string(Matchers.containsString("Yêu cầu giảng viên hướng dẫn")));
	}

	@Test
	void validRoomFormRequiresCsrfAndRedirectsAfterCreation() throws Exception {
		mockMvc.perform(post("/catalog/rooms").with(user("manager").roles("CBQL")).param("id", "P-NEW")
				.param("name", "Phòng mới").param("groupId", "NP01").param("location", "Tầng 2").param("capacity", "24")
				.param("status", "SAN_SANG")).andExpect(status().isForbidden());

		mockMvc.perform(post("/catalog/rooms").with(user("manager").roles("CBQL")).with(csrf()).param("id", "P-NEW")
				.param("name", "Phòng mới").param("groupId", "NP01").param("location", "Tầng 2").param("capacity", "24")
				.param("status", "SAN_SANG")).andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/catalog/rooms"));
		verify(catalogService).createRoom(any());
	}

	@Test
	void invalidCapacityReturnsInlineFormWithoutCallingService() throws Exception {
		mockMvc.perform(post("/catalog/rooms").with(user("manager").roles("CBQL")).with(csrf()).param("id", "P-BAD")
				.param("name", "Phòng lỗi").param("groupId", "NP01").param("location", "Tầng 2").param("capacity", "0")
				.param("status", "SAN_SANG")).andExpect(status().isOk()).andExpect(view().name("catalog/room-form"))
				.andExpect(content().string(Matchers.containsString("giá trị phải lớn hơn 0")));
		verify(catalogService, never()).createRoom(any());
	}
}
