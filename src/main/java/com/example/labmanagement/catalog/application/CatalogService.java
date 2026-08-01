package com.example.labmanagement.catalog.application;

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
import com.example.labmanagement.common.error.ErrorCode;
import com.example.labmanagement.registration.domain.PhieuDangKyTrangThai;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {

	private static final Map<String, String> ROOM_SORT_FIELDS = Map.of("id", "id", "name", "name", "group",
			"group.name", "location", "location", "capacity", "capacity", "status", "status");
	private static final Map<String, String> DEVICE_SORT_FIELDS = Map.of("id", "id", "name", "name", "type",
			"type.name", "serialNumber", "serialNumber", "model", "model", "room", "room.name", "status", "status");
	private static final Set<ThietBiTrangThai> ALLOCATION_BLOCKING_DEVICE_STATUSES = Set.of(ThietBiTrangThai.HONG,
			ThietBiTrangThai.BAO_TRI, ThietBiTrangThai.NGUNG_SU_DUNG);
	private static final Set<PhieuDangKyTrangThai> ACTIVE_REGISTRATION_STATUSES = Set.of(PhieuDangKyTrangThai.DA_DUYET,
			PhieuDangKyTrangThai.DANG_SU_DUNG);

	private final NhomPhongRepository roomGroupRepository;
	private final PhongRepository roomRepository;
	private final LoaiThietBiRepository deviceTypeRepository;
	private final ThietBiRepository deviceRepository;
	private final TaiNguyenRepository resourceRepository;

	public CatalogService(NhomPhongRepository roomGroupRepository, PhongRepository roomRepository,
			LoaiThietBiRepository deviceTypeRepository, ThietBiRepository deviceRepository,
			TaiNguyenRepository resourceRepository) {
		this.roomGroupRepository = roomGroupRepository;
		this.roomRepository = roomRepository;
		this.deviceTypeRepository = deviceTypeRepository;
		this.deviceRepository = deviceRepository;
		this.resourceRepository = resourceRepository;
	}

	@Transactional(readOnly = true)
	public Page<RoomResponse> searchRooms(String groupId, PhongTrangThai status, String keyword, int page, int size,
			String sort) {
		Pageable pageable = pageRequest(page, size, sort, ROOM_SORT_FIELDS);
		return roomRepository.search(normalizeOptional(groupId), status, normalizeOptional(keyword), pageable)
				.map(this::toRoomResponse);
	}

	@Transactional(readOnly = true)
	public Page<DeviceResponse> searchDevices(String typeId, String roomId, ThietBiTrangThai status, String keyword,
			int page, int size, String sort) {
		Pageable pageable = pageRequest(page, size, sort, DEVICE_SORT_FIELDS);
		return deviceRepository.search(normalizeOptional(typeId), normalizeOptional(roomId), status,
				normalizeOptional(keyword), pageable).map(this::toDeviceResponse);
	}

	@Transactional(readOnly = true)
	public RoomResponse getRoom(String id) {
		return toRoomResponse(findRoom(id));
	}

	@Transactional(readOnly = true)
	public DeviceResponse getDevice(String id) {
		return toDeviceResponse(findDevice(id));
	}

	@Transactional
	public RoomResponse createRoom(RoomCreateRequest request) {
		String id = normalizeId(request.id());
		if (roomRepository.existsById(id)) {
			throw conflict("Mã phòng đã tồn tại.");
		}
		NhomPhong group = findRoomGroup(request.groupId());
		Phong room = new Phong(id, normalizeRequired(request.name()), group, normalizeRequired(request.location()),
				request.capacity(), request.status());
		try {
			roomRepository.saveAndFlush(room);
			resourceRepository.saveAndFlush(TaiNguyen.forRoom(newResourceId(), room));
			return toRoomResponse(room);
		} catch (DataIntegrityViolationException exception) {
			throw conflict("Không thể tạo phòng do mã hoặc dữ liệu liên kết bị trùng.");
		}
	}

	@Transactional
	public RoomResponse updateRoom(String id, RoomUpdateRequest request) {
		Phong room = findRoom(id);
		if (room.getVersion() != request.version()) {
			throw conflict("Dữ liệu phòng đã được cập nhật bởi yêu cầu khác.");
		}
		NhomPhong group = findRoomGroup(request.groupId());
		room.update(normalizeRequired(request.name()), group, normalizeRequired(request.location()), request.capacity(),
				request.status());
		try {
			roomRepository.flush();
			return toRoomResponse(room);
		} catch (DataIntegrityViolationException exception) {
			throw conflict("Không thể cập nhật phòng do dữ liệu liên kết không hợp lệ.");
		}
	}

	@Transactional
	public void deleteRoom(String id) {
		Phong room = findRoom(id);
		if (deviceRepository.countByRoom_Id(room.getId()) > 0) {
			throw conflict("Không thể xóa phòng đang được gán thiết bị. Hãy chuyển trạng thái ngừng sử dụng.");
		}
		try {
			resourceRepository.findByRoom_Id(room.getId()).ifPresent(resourceRepository::delete);
			resourceRepository.flush();
			roomRepository.delete(room);
			roomRepository.flush();
		} catch (DataIntegrityViolationException exception) {
			throw conflict("Phòng đã có dữ liệu nghiệp vụ; chỉ được chuyển trạng thái ngừng sử dụng.");
		}
	}

	@Transactional
	public DeviceResponse createDevice(DeviceCreateRequest request) {
		String id = normalizeId(request.id());
		String serialNumber = normalizeOptional(request.serialNumber());
		if (deviceRepository.existsById(id)) {
			throw conflict("Mã thiết bị đã tồn tại.");
		}
		if (serialNumber != null && deviceRepository.findBySerialNumber(serialNumber).isPresent()) {
			throw conflict("Số serial đã được sử dụng.");
		}
		LoaiThietBi type = findDeviceType(request.typeId());
		Phong room = findOptionalRoom(request.roomId());
		ThietBi device = new ThietBi(id, normalizeRequired(request.name()), type, serialNumber,
				normalizeOptional(request.model()), room, request.status());
		try {
			deviceRepository.saveAndFlush(device);
			resourceRepository.saveAndFlush(TaiNguyen.forDevice(newResourceId(), device));
			return toDeviceResponse(device);
		} catch (DataIntegrityViolationException exception) {
			throw conflict("Không thể tạo thiết bị do mã hoặc số serial bị trùng.");
		}
	}

	@Transactional
	public DeviceResponse updateDevice(String id, DeviceUpdateRequest request) {
		ThietBi device = deviceRepository.findByIdForUpdate(normalizeId(id)).orElseThrow(
				() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Không tìm thấy thiết bị."));
		resourceRepository.lockForApproval("__NO_ROOM__", List.of(device.getId()));
		if (device.getVersion() != request.version()) {
			throw conflict("Dữ liệu thiết bị đã được cập nhật bởi yêu cầu khác.");
		}
		if (ALLOCATION_BLOCKING_DEVICE_STATUSES.contains(request.status())
				&& deviceRepository.existsActiveAllocation(device.getId(), ACTIVE_REGISTRATION_STATUSES)) {
			throw conflict("Không thể chuyển thiết bị đang được phân bổ cho phiếu còn hiệu lực sang trạng thái này.");
		}
		String serialNumber = normalizeOptional(request.serialNumber());
		if (serialNumber != null && deviceRepository.existsBySerialNumberAndIdNot(serialNumber, device.getId())) {
			throw conflict("Số serial đã được sử dụng.");
		}
		LoaiThietBi type = findDeviceType(request.typeId());
		Phong room = findOptionalRoom(request.roomId());
		device.update(normalizeRequired(request.name()), type, serialNumber, normalizeOptional(request.model()), room,
				request.status());
		try {
			deviceRepository.flush();
			return toDeviceResponse(device);
		} catch (DataIntegrityViolationException exception) {
			throw conflict("Không thể cập nhật thiết bị do số serial bị trùng.");
		}
	}

	@Transactional
	public void deleteDevice(String id) {
		ThietBi device = findDevice(id);
		try {
			resourceRepository.findByDevice_Id(device.getId()).ifPresent(resourceRepository::delete);
			resourceRepository.flush();
			deviceRepository.delete(device);
			deviceRepository.flush();
		} catch (DataIntegrityViolationException exception) {
			throw conflict("Thiết bị đã có dữ liệu nghiệp vụ; chỉ được chuyển trạng thái ngừng sử dụng.");
		}
	}

	@Transactional(readOnly = true)
	public List<RoomGroupResponse> roomGroups() {
		return roomGroupRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream().map(this::toRoomGroupResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public RoomGroupResponse getRoomGroup(String id) {
		return toRoomGroupResponse(findRoomGroup(id));
	}

	@Transactional
	public RoomGroupResponse createRoomGroup(RoomGroupRequest request) {
		String id = normalizeId(request.id());
		String name = normalizeRequired(request.name());
		if (roomGroupRepository.existsById(id) || roomGroupRepository.existsByNameIgnoreCase(name)) {
			throw conflict("Mã hoặc tên nhóm phòng đã tồn tại.");
		}
		try {
			return toRoomGroupResponse(roomGroupRepository
					.saveAndFlush(new NhomPhong(id, name, normalizeOptional(request.description()))));
		} catch (DataIntegrityViolationException exception) {
			throw conflict("Mã hoặc tên nhóm phòng đã tồn tại.");
		}
	}

	@Transactional
	public RoomGroupResponse updateRoomGroup(String id, RoomGroupRequest request) {
		NhomPhong group = findRoomGroup(id);
		String name = normalizeRequired(request.name());
		if (roomGroupRepository.existsByNameIgnoreCaseAndIdNot(name, group.getId())) {
			throw conflict("Tên nhóm phòng đã tồn tại.");
		}
		group.update(name, normalizeOptional(request.description()));
		try {
			roomGroupRepository.flush();
			return toRoomGroupResponse(group);
		} catch (DataIntegrityViolationException exception) {
			throw conflict("Tên nhóm phòng đã tồn tại.");
		}
	}

	@Transactional
	public void deleteRoomGroup(String id) {
		NhomPhong group = findRoomGroup(id);
		if (roomRepository.countByGroup_Id(group.getId()) > 0) {
			throw conflict("Không thể xóa nhóm phòng đang có phòng.");
		}
		roomGroupRepository.delete(group);
	}

	@Transactional(readOnly = true)
	public List<DeviceTypeResponse> deviceTypes() {
		return deviceTypeRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
				.map(this::toDeviceTypeResponse).toList();
	}

	@Transactional(readOnly = true)
	public DeviceTypeResponse getDeviceType(String id) {
		return toDeviceTypeResponse(findDeviceType(id));
	}

	@Transactional
	public DeviceTypeResponse createDeviceType(DeviceTypeRequest request) {
		String id = normalizeId(request.id());
		String name = normalizeRequired(request.name());
		if (deviceTypeRepository.existsById(id) || deviceTypeRepository.existsByNameIgnoreCase(name)) {
			throw conflict("Mã hoặc tên loại thiết bị đã tồn tại.");
		}
		LoaiThietBi type = new LoaiThietBi(id, name, request.instructorRequired(), request.mobile(),
				normalizeOptional(request.description()));
		try {
			return toDeviceTypeResponse(deviceTypeRepository.saveAndFlush(type));
		} catch (DataIntegrityViolationException exception) {
			throw conflict("Mã hoặc tên loại thiết bị đã tồn tại.");
		}
	}

	@Transactional
	public DeviceTypeResponse updateDeviceType(String id, DeviceTypeRequest request) {
		LoaiThietBi type = findDeviceType(id);
		String name = normalizeRequired(request.name());
		if (deviceTypeRepository.existsByNameIgnoreCaseAndIdNot(name, type.getId())) {
			throw conflict("Tên loại thiết bị đã tồn tại.");
		}
		type.update(name, request.instructorRequired(), request.mobile(), normalizeOptional(request.description()));
		try {
			deviceTypeRepository.flush();
			return toDeviceTypeResponse(type);
		} catch (DataIntegrityViolationException exception) {
			throw conflict("Tên loại thiết bị đã tồn tại.");
		}
	}

	@Transactional
	public void deleteDeviceType(String id) {
		LoaiThietBi type = findDeviceType(id);
		if (deviceRepository.countByType_Id(type.getId()) > 0) {
			throw conflict("Không thể xóa loại thiết bị đang được sử dụng.");
		}
		deviceTypeRepository.delete(type);
	}

	@Transactional(readOnly = true)
	public List<RoomResponse> selectableRooms() {
		return roomRepository.findAllByStatusNotOrderByNameAsc(PhongTrangThai.NGUNG_SU_DUNG).stream()
				.map(this::toRoomResponse).toList();
	}

	@Transactional(readOnly = true)
	public List<RoomResponse> roomsForFilter() {
		return roomRepository.findAllByOrderByNameAsc().stream().map(this::toRoomResponse).toList();
	}

	@Transactional(readOnly = true)
	public List<DeviceResponse> selectableDevices() {
		return deviceRepository.findAllByStatusNotOrderByNameAsc(ThietBiTrangThai.NGUNG_SU_DUNG).stream()
				.map(this::toDeviceResponse).toList();
	}

	public String normalizeRoomSort(String sort) {
		return normalizeSort(sort, ROOM_SORT_FIELDS);
	}

	public String normalizeDeviceSort(String sort) {
		return normalizeSort(sort, DEVICE_SORT_FIELDS);
	}

	private Pageable pageRequest(int page, int size, String sort, Map<String, String> allowedFields) {
		String normalizedSort = normalizeSort(sort, allowedFields);
		String[] parts = normalizedSort.split(",");
		Sort.Direction direction = Sort.Direction.fromString(parts[1]);
		return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), direction,
				allowedFields.get(parts[0]));
	}

	private String normalizeSort(String sort, Map<String, String> allowedFields) {
		String value = sort == null || sort.isBlank() ? "id,asc" : sort.trim();
		String[] parts = value.split(",", -1);
		if (parts.length != 2 || !allowedFields.containsKey(parts[0])
				|| !("asc".equalsIgnoreCase(parts[1]) || "desc".equalsIgnoreCase(parts[1]))) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST,
					"Tham số sort phải có dạng field,asc hoặc field,desc với trường được hỗ trợ.");
		}
		return parts[0] + "," + parts[1].toLowerCase();
	}

	private NhomPhong findRoomGroup(String id) {
		return roomGroupRepository.findById(normalizeId(id)).orElseThrow(
				() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Không tìm thấy nhóm phòng."));
	}

	private Phong findRoom(String id) {
		return roomRepository.findById(normalizeId(id)).orElseThrow(
				() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Không tìm thấy phòng."));
	}

	private Phong findOptionalRoom(String id) {
		String normalized = normalizeOptional(id);
		return normalized == null ? null : findRoom(normalized);
	}

	private LoaiThietBi findDeviceType(String id) {
		return deviceTypeRepository.findById(normalizeId(id)).orElseThrow(
				() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Không tìm thấy loại thiết bị."));
	}

	private ThietBi findDevice(String id) {
		return deviceRepository.findById(normalizeId(id)).orElseThrow(
				() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Không tìm thấy thiết bị."));
	}

	private RoomResponse toRoomResponse(Phong room) {
		return new RoomResponse(room.getId(), room.getName(), room.getGroup().getId(), room.getGroup().getName(),
				room.getLocation(), room.getCapacity(), room.getStatus(), room.getVersion());
	}

	private DeviceResponse toDeviceResponse(ThietBi device) {
		Phong room = device.getRoom();
		return new DeviceResponse(device.getId(), device.getName(), device.getType().getId(),
				device.getType().getName(), device.getType().isInstructorRequired(), device.getType().isMobile(),
				device.getSerialNumber(), device.getModel(), room == null ? null : room.getId(),
				room == null ? null : room.getName(), device.getStatus(), device.getVersion());
	}

	private RoomGroupResponse toRoomGroupResponse(NhomPhong group) {
		return new RoomGroupResponse(group.getId(), group.getName(), group.getDescription());
	}

	private DeviceTypeResponse toDeviceTypeResponse(LoaiThietBi type) {
		return new DeviceTypeResponse(type.getId(), type.getName(), type.isInstructorRequired(), type.isMobile(),
				type.getDescription());
	}

	private String normalizeId(String value) {
		return normalizeRequired(value);
	}

	private String normalizeRequired(String value) {
		return value == null ? "" : value.trim();
	}

	private String normalizeOptional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private String newResourceId() {
		return "TN-" + UUID.randomUUID();
	}

	private ApiException conflict(String message) {
		return new ApiException(ErrorCode.RESOURCE_CONFLICT, HttpStatus.CONFLICT, message);
	}
}
