package com.example.labmanagement.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.labmanagement.catalog.domain.PhongTrangThai;
import com.example.labmanagement.catalog.domain.ThietBiTrangThai;
import com.example.labmanagement.catalog.dto.DeviceTypeRequest;
import com.example.labmanagement.catalog.dto.DeviceTypeResponse;
import com.example.labmanagement.catalog.dto.RoomGroupRequest;
import com.example.labmanagement.catalog.dto.RoomGroupResponse;
import com.example.labmanagement.catalog.repository.PhongRepository;
import com.example.labmanagement.catalog.repository.TaiNguyenRepository;
import com.example.labmanagement.catalog.repository.ThietBiRepository;
import com.example.labmanagement.catalog.service.CatalogService;
import com.example.labmanagement.common.error.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** MySQL acceptance for TC-CAT-01..14, FR-05..07 and API-09..14. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "LAB_TEST_DB_PASSWORD", matches = ".+")
@Transactional
class CatalogMySqlIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CatalogService catalogService;

	@Autowired
	private PhongRepository roomRepository;

	@Autowired
	private ThietBiRepository deviceRepository;

	@Autowired
	private TaiNguyenRepository resourceRepository;

	@Test
	void apiCreatesRoomAndDeviceWithExactlyOneResourceEachAndRejectsDuplicateSerial() throws Exception {
		catalogService.createRoomGroup(new RoomGroupRequest("S3-NP", "Nhóm Giai đoạn 3", null));
		catalogService.createDeviceType(new DeviceTypeRequest("S3-TYPE", "Loại Giai đoạn 3", true, true, null));

		mockMvc.perform(post("/api/v1/rooms").with(user("manager").roles("CBQL")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"id":"S3-ROOM","name":"Phòng Giai đoạn 3","groupId":"S3-NP","location":"Tầng test",
						 "capacity":20,"status":"SAN_SANG"}
						""")).andExpect(status().isCreated()).andExpect(jsonPath("$.data.id").value("S3-ROOM"));
		assertThat(resourceRepository.countByRoom_Id("S3-ROOM")).isEqualTo(1);

		String firstDevice = """
				{"id":"S3-DEVICE","name":"Thiết bị Giai đoạn 3","typeId":"S3-TYPE","serialNumber":"S3-SERIAL",
				 "model":"S3","roomId":"S3-ROOM","status":"SAN_SANG"}
				""";
		mockMvc.perform(post("/api/v1/devices").with(user("manager").roles("CBQL")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(firstDevice)).andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.instructorRequired").value(true))
				.andExpect(jsonPath("$.data.mobile").value(true));
		assertThat(resourceRepository.countByDevice_Id("S3-DEVICE")).isEqualTo(1);

		mockMvc.perform(post("/api/v1/devices").with(user("manager").roles("CBQL")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(firstDevice.replace("S3-DEVICE", "S3-DUPLICATE")))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));
		assertThat(deviceRepository.findById("S3-DUPLICATE")).isEmpty();
		assertThat(resourceRepository.countByDevice_Id("S3-DUPLICATE")).isZero();
	}

	@Test
	void apiFiltersPagesAndSortsSeededCatalogOnTheServer() throws Exception {
		mockMvc.perform(get("/api/v1/rooms").with(user("student").roles("SV")).param("group", "NP01")
				.param("keyword", "P0").param("page", "0").param("size", "3").param("sort", "capacity,desc"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(3))
				.andExpect(jsonPath("$.data[0].groupId").value("NP01"))
				.andExpect(jsonPath("$.meta.totalElements").value(4))
				.andExpect(jsonPath("$.meta.sort").value("capacity,desc"));

		mockMvc.perform(get("/api/v1/devices").with(user("lecturer").roles("GV")).param("type", "ROBOT")
				.param("room", "P0604").param("keyword", "SERIAL").param("sort", "serialNumber,asc"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data").isNotEmpty())
				.andExpect(jsonPath("$.data[0].typeId").value("ROBOT"))
				.andExpect(jsonPath("$.data[0].roomId").value("P0604"));
	}

	@Test
	void roomWithBusinessDataCannotBeHardDeletedAndStoppedResourcesAreNotSelectable() {
		assertThatThrownBy(() -> catalogService.deleteRoom("P0601")).isInstanceOf(ApiException.class)
				.hasMessageContaining("ngừng sử dụng");
		assertThat(roomRepository.existsById("P0601")).isTrue();

		assertThat(catalogService.selectableRooms()).noneMatch(room -> room.status() == PhongTrangThai.NGUNG_SU_DUNG);
		assertThat(catalogService.selectableDevices())
				.noneMatch(device -> device.status() == ThietBiTrangThai.NGUNG_SU_DUNG);
		assertThat(catalogService.selectableDevices()).hasSize(295);
	}

	@Test
	void managerCanCreateUpdateAndDeleteUnusedGroupsAndTypes() {
		RoomGroupResponse group = catalogService
				.createRoomGroup(new RoomGroupRequest("S3-GROUP-CRUD", "Nhóm CRUD", "Ban đầu"));
		assertThat(group.description()).isEqualTo("Ban đầu");
		group = catalogService.updateRoomGroup(group.id(), new RoomGroupRequest(group.id(), "Nhóm CRUD mới", "Đã sửa"));
		assertThat(group.name()).isEqualTo("Nhóm CRUD mới");
		catalogService.deleteRoomGroup(group.id());

		DeviceTypeResponse type = catalogService
				.createDeviceType(new DeviceTypeRequest("S3-TYPE-CRUD", "Loại CRUD", true, false, "Ban đầu"));
		assertThat(type.instructorRequired()).isTrue();
		assertThat(type.mobile()).isFalse();
		type = catalogService.updateDeviceType(type.id(),
				new DeviceTypeRequest(type.id(), "Loại CRUD mới", false, true, "Đã sửa"));
		assertThat(type.instructorRequired()).isFalse();
		assertThat(type.mobile()).isTrue();
		catalogService.deleteDeviceType(type.id());
	}
}
