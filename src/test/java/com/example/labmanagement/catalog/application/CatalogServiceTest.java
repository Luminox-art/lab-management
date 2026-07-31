package com.example.labmanagement.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.labmanagement.catalog.domain.LoaiThietBi;
import com.example.labmanagement.catalog.domain.NhomPhong;
import com.example.labmanagement.catalog.domain.Phong;
import com.example.labmanagement.catalog.domain.PhongTrangThai;
import com.example.labmanagement.catalog.domain.TaiNguyen;
import com.example.labmanagement.catalog.domain.ThietBi;
import com.example.labmanagement.catalog.domain.ThietBiTrangThai;
import com.example.labmanagement.catalog.persistence.LoaiThietBiRepository;
import com.example.labmanagement.catalog.persistence.NhomPhongRepository;
import com.example.labmanagement.catalog.persistence.PhongRepository;
import com.example.labmanagement.catalog.persistence.TaiNguyenRepository;
import com.example.labmanagement.catalog.persistence.ThietBiRepository;
import com.example.labmanagement.common.error.ApiException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** TC-CAT-01..14 for FR-05..07, UC-05/17/18 and API-09..14. */
@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

	@Mock
	private NhomPhongRepository roomGroupRepository;
	@Mock
	private PhongRepository roomRepository;
	@Mock
	private LoaiThietBiRepository deviceTypeRepository;
	@Mock
	private ThietBiRepository deviceRepository;
	@Mock
	private TaiNguyenRepository resourceRepository;

	private CatalogService service;

	@BeforeEach
	void setUp() {
		service = new CatalogService(roomGroupRepository, roomRepository, deviceTypeRepository, deviceRepository,
				resourceRepository);
	}

	@Test
	void createsRoomAndExactlyOneLinkedResourceInTheSameServiceOperation() {
		NhomPhong group = new NhomPhong("NP-TEST", "Nhóm kiểm thử", null);
		when(roomGroupRepository.findById("NP-TEST")).thenReturn(Optional.of(group));
		when(roomRepository.saveAndFlush(any(Phong.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(resourceRepository.saveAndFlush(any(TaiNguyen.class))).thenAnswer(invocation -> invocation.getArgument(0));

		RoomResponse result = service.createRoom(new RoomCreateRequest(" P-TEST ", " Phòng kiểm thử ", "NP-TEST",
				" Tầng 1 ", 24, PhongTrangThai.SAN_SANG));

		assertThat(result.id()).isEqualTo("P-TEST");
		assertThat(result.capacity()).isEqualTo(24);
		ArgumentCaptor<TaiNguyen> resource = ArgumentCaptor.forClass(TaiNguyen.class);
		verify(resourceRepository).saveAndFlush(resource.capture());
		assertThat(resource.getValue().getRoom().getId()).isEqualTo("P-TEST");
		assertThat(resource.getValue().getDevice()).isNull();
	}

	@Test
	void createsDeviceWithNullableSerialAndLinkedResource() {
		LoaiThietBi type = new LoaiThietBi("PC", "Máy tính", true, false, null);
		when(deviceTypeRepository.findById("PC")).thenReturn(Optional.of(type));
		when(deviceRepository.saveAndFlush(any(ThietBi.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(resourceRepository.saveAndFlush(any(TaiNguyen.class))).thenAnswer(invocation -> invocation.getArgument(0));

		DeviceResponse result = service.createDevice(new DeviceCreateRequest("TB-TEST", "Thiết bị", "PC", " ",
				" Model A ", null, ThietBiTrangThai.SAN_SANG));

		assertThat(result.serialNumber()).isNull();
		assertThat(result.instructorRequired()).isTrue();
		ArgumentCaptor<TaiNguyen> resource = ArgumentCaptor.forClass(TaiNguyen.class);
		verify(resourceRepository).saveAndFlush(resource.capture());
		assertThat(resource.getValue().getDevice().getId()).isEqualTo("TB-TEST");
	}

	@Test
	void rejectsDuplicateNonNullSerialBeforeSavingDevice() {
		LoaiThietBi type = new LoaiThietBi("PC", "Máy tính", false, false, null);
		ThietBi existing = new ThietBi("TB-OLD", "Cũ", type, "SERIAL-1", null, null, ThietBiTrangThai.SAN_SANG);
		when(deviceRepository.findBySerialNumber("SERIAL-1")).thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> service.createDevice(
				new DeviceCreateRequest("TB-NEW", "Mới", "PC", "SERIAL-1", null, null, ThietBiTrangThai.SAN_SANG)))
				.isInstanceOf(ApiException.class).hasMessage("Số serial đã được sử dụng.");
		verify(deviceRepository, never()).saveAndFlush(any());
		verify(resourceRepository, never()).saveAndFlush(any());
	}

	@Test
	void rejectsStaleRoomVersion() {
		NhomPhong group = new NhomPhong("NP", "Nhóm", null);
		Phong room = new Phong("P1", "Phòng", group, "Tầng 1", 20, PhongTrangThai.SAN_SANG);
		when(roomRepository.findById("P1")).thenReturn(Optional.of(room));

		assertThatThrownBy(() -> service.updateRoom("P1",
				new RoomUpdateRequest("Tên mới", "NP", "Tầng 2", 30, PhongTrangThai.BAO_TRI, 1)))
				.isInstanceOf(ApiException.class).hasMessageContaining("yêu cầu khác");
		verify(roomGroupRepository, never()).findById(any());
	}

	@Test
	void preventsHardDeleteWhenRoomStillContainsDevices() {
		NhomPhong group = new NhomPhong("NP", "Nhóm", null);
		Phong room = new Phong("P1", "Phòng", group, "Tầng 1", 20, PhongTrangThai.SAN_SANG);
		when(roomRepository.findById("P1")).thenReturn(Optional.of(room));
		when(deviceRepository.countByRoom_Id("P1")).thenReturn(1L);

		assertThatThrownBy(() -> service.deleteRoom("P1")).isInstanceOf(ApiException.class)
				.hasMessageContaining("ngừng sử dụng");
		verify(resourceRepository, never()).delete(any());
		verify(roomRepository, never()).delete(any());
	}

	@Test
	void selectionListsExcludeStoppedResources() {
		when(roomRepository.findAllByStatusNotOrderByNameAsc(PhongTrangThai.NGUNG_SU_DUNG)).thenReturn(List.of());
		when(deviceRepository.findAllByStatusNotOrderByNameAsc(ThietBiTrangThai.NGUNG_SU_DUNG)).thenReturn(List.of());

		assertThat(service.selectableRooms()).isEmpty();
		assertThat(service.selectableDevices()).isEmpty();
		verify(roomRepository).findAllByStatusNotOrderByNameAsc(PhongTrangThai.NGUNG_SU_DUNG);
		verify(deviceRepository).findAllByStatusNotOrderByNameAsc(ThietBiTrangThai.NGUNG_SU_DUNG);
	}

	@Test
	void rejectsUnsupportedServerSortField() {
		assertThatThrownBy(() -> service.searchRooms(null, null, null, 0, 20, "unknown,asc"))
				.isInstanceOf(ApiException.class).hasMessageContaining("field,asc");
		verify(roomRepository, never()).search(any(), any(), any(), any());
	}
}
