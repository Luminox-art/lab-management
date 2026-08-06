package com.example.labmanagement.catalog.controller;

import com.example.labmanagement.catalog.domain.PhongTrangThai;
import com.example.labmanagement.catalog.domain.ThietBiTrangThai;
import com.example.labmanagement.catalog.dto.CatalogForms;
import com.example.labmanagement.catalog.dto.DeviceResponse;
import com.example.labmanagement.catalog.dto.RoomResponse;
import com.example.labmanagement.catalog.service.CatalogService;
import com.example.labmanagement.common.error.ApiException;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/catalog")
public class CatalogWebController {

	private final CatalogService catalogService;

	public CatalogWebController(CatalogService catalogService) {
		this.catalogService = catalogService;
	}

	@GetMapping("/rooms")
	String rooms(@RequestParam(required = false) String group, @RequestParam(required = false) PhongTrangThai status,
			@RequestParam(required = false) String keyword, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "id,asc") String sort,
			Authentication authentication, Model model) {
		Page<RoomResponse> result = catalogService.searchRooms(group, status, keyword, page, size, sort);
		model.addAttribute("result", result);
		model.addAttribute("groups", catalogService.roomGroups());
		model.addAttribute("statuses", PhongTrangThai.values());
		model.addAttribute("group", group);
		model.addAttribute("status", status);
		model.addAttribute("keyword", keyword);
		model.addAttribute("sort", catalogService.normalizeRoomSort(sort));
		model.addAttribute("canManage", isManager(authentication));
		return "catalog/rooms";
	}

	@GetMapping("/devices")
	String devices(@RequestParam(required = false) String type, @RequestParam(required = false) String room,
			@RequestParam(required = false) ThietBiTrangThai status, @RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "id,asc") String sort, Authentication authentication, Model model) {
		Page<DeviceResponse> result = catalogService.searchDevices(type, room, status, keyword, page, size, sort);
		model.addAttribute("result", result);
		model.addAttribute("types", catalogService.deviceTypes());
		model.addAttribute("rooms", catalogService.roomsForFilter());
		model.addAttribute("statuses", ThietBiTrangThai.values());
		model.addAttribute("type", type);
		model.addAttribute("room", room);
		model.addAttribute("status", status);
		model.addAttribute("keyword", keyword);
		model.addAttribute("sort", catalogService.normalizeDeviceSort(sort));
		model.addAttribute("canManage", isManager(authentication));
		return "catalog/devices";
	}

	@GetMapping("/rooms/new")
	String newRoom(Model model) {
		model.addAttribute("roomForm", new CatalogForms.RoomForm());
		return roomForm(model, false);
	}

	@GetMapping("/rooms/{id}/edit")
	String editRoom(@PathVariable String id, Model model) {
		model.addAttribute("roomForm", CatalogForms.RoomForm.from(catalogService.getRoom(id)));
		return roomForm(model, true);
	}

	@PostMapping("/rooms")
	String createRoom(@Valid @ModelAttribute("roomForm") CatalogForms.RoomForm form, BindingResult bindingResult,
			Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			return roomForm(model, false);
		}
		try {
			catalogService.createRoom(form.toCreateRequest());
			redirectAttributes.addFlashAttribute("success", "Đã tạo phòng và tài nguyên liên kết.");
			return "redirect:/catalog/rooms";
		} catch (ApiException exception) {
			bindingResult.reject("catalog.room", exception.getMessage());
			return roomForm(model, false);
		}
	}

	@PostMapping("/rooms/{id}")
	String updateRoom(@PathVariable String id, @Valid @ModelAttribute("roomForm") CatalogForms.RoomForm form,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			return roomForm(model, true);
		}
		try {
			catalogService.updateRoom(id, form.toUpdateRequest());
			redirectAttributes.addFlashAttribute("success", "Đã cập nhật phòng.");
			return "redirect:/catalog/rooms";
		} catch (ApiException exception) {
			bindingResult.reject("catalog.room", exception.getMessage());
			return roomForm(model, true);
		}
	}

	@PostMapping("/rooms/{id}/delete")
	String deleteRoom(@PathVariable String id, RedirectAttributes redirectAttributes) {
		try {
			catalogService.deleteRoom(id);
			redirectAttributes.addFlashAttribute("success", "Đã xóa phòng chưa phát sinh dữ liệu.");
		} catch (ApiException exception) {
			redirectAttributes.addFlashAttribute("error", exception.getMessage());
		}
		return "redirect:/catalog/rooms";
	}

	@GetMapping("/devices/new")
	String newDevice(Model model) {
		model.addAttribute("deviceForm", new CatalogForms.DeviceForm());
		return deviceForm(model, false);
	}

	@GetMapping("/devices/{id}/edit")
	String editDevice(@PathVariable String id, Model model) {
		model.addAttribute("deviceForm", CatalogForms.DeviceForm.from(catalogService.getDevice(id)));
		return deviceForm(model, true);
	}

	@PostMapping("/devices")
	String createDevice(@Valid @ModelAttribute("deviceForm") CatalogForms.DeviceForm form, BindingResult bindingResult,
			Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			return deviceForm(model, false);
		}
		try {
			catalogService.createDevice(form.toCreateRequest());
			redirectAttributes.addFlashAttribute("success", "Đã tạo thiết bị và tài nguyên liên kết.");
			return "redirect:/catalog/devices";
		} catch (ApiException exception) {
			bindingResult.reject("catalog.device", exception.getMessage());
			return deviceForm(model, false);
		}
	}

	@PostMapping("/devices/{id}")
	String updateDevice(@PathVariable String id, @Valid @ModelAttribute("deviceForm") CatalogForms.DeviceForm form,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			return deviceForm(model, true);
		}
		try {
			catalogService.updateDevice(id, form.toUpdateRequest());
			redirectAttributes.addFlashAttribute("success", "Đã cập nhật thiết bị.");
			return "redirect:/catalog/devices";
		} catch (ApiException exception) {
			bindingResult.reject("catalog.device", exception.getMessage());
			return deviceForm(model, true);
		}
	}

	@PostMapping("/devices/{id}/delete")
	String deleteDevice(@PathVariable String id, RedirectAttributes redirectAttributes) {
		try {
			catalogService.deleteDevice(id);
			redirectAttributes.addFlashAttribute("success", "Đã xóa thiết bị chưa phát sinh dữ liệu.");
		} catch (ApiException exception) {
			redirectAttributes.addFlashAttribute("error", exception.getMessage());
		}
		return "redirect:/catalog/devices";
	}

	@GetMapping("/room-groups")
	String roomGroups(@RequestParam(required = false) String edit, Model model) {
		CatalogForms.RoomGroupForm form = edit == null
				? new CatalogForms.RoomGroupForm()
				: CatalogForms.RoomGroupForm.from(catalogService.getRoomGroup(edit));
		model.addAttribute("groupForm", form);
		return roomGroups(model);
	}

	@PostMapping("/room-groups")
	String saveRoomGroup(@Valid @ModelAttribute("groupForm") CatalogForms.RoomGroupForm form,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			return roomGroups(model);
		}
		try {
			if (form.getOriginalId() == null || form.getOriginalId().isBlank()) {
				catalogService.createRoomGroup(form.toRequest());
			} else {
				catalogService.updateRoomGroup(form.getOriginalId(), form.toRequest());
			}
			redirectAttributes.addFlashAttribute("success", "Đã lưu nhóm phòng.");
			return "redirect:/catalog/room-groups";
		} catch (ApiException exception) {
			bindingResult.reject("catalog.group", exception.getMessage());
			return roomGroups(model);
		}
	}

	@PostMapping("/room-groups/{id}/delete")
	String deleteRoomGroup(@PathVariable String id, RedirectAttributes redirectAttributes) {
		try {
			catalogService.deleteRoomGroup(id);
			redirectAttributes.addFlashAttribute("success", "Đã xóa nhóm phòng.");
		} catch (ApiException exception) {
			redirectAttributes.addFlashAttribute("error", exception.getMessage());
		}
		return "redirect:/catalog/room-groups";
	}

	@GetMapping("/device-types")
	String deviceTypes(@RequestParam(required = false) String edit, Model model) {
		CatalogForms.DeviceTypeForm form = edit == null
				? new CatalogForms.DeviceTypeForm()
				: CatalogForms.DeviceTypeForm.from(catalogService.getDeviceType(edit));
		model.addAttribute("typeForm", form);
		return deviceTypes(model);
	}

	@PostMapping("/device-types")
	String saveDeviceType(@Valid @ModelAttribute("typeForm") CatalogForms.DeviceTypeForm form,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			return deviceTypes(model);
		}
		try {
			if (form.getOriginalId() == null || form.getOriginalId().isBlank()) {
				catalogService.createDeviceType(form.toRequest());
			} else {
				catalogService.updateDeviceType(form.getOriginalId(), form.toRequest());
			}
			redirectAttributes.addFlashAttribute("success", "Đã lưu loại thiết bị.");
			return "redirect:/catalog/device-types";
		} catch (ApiException exception) {
			bindingResult.reject("catalog.type", exception.getMessage());
			return deviceTypes(model);
		}
	}

	@PostMapping("/device-types/{id}/delete")
	String deleteDeviceType(@PathVariable String id, RedirectAttributes redirectAttributes) {
		try {
			catalogService.deleteDeviceType(id);
			redirectAttributes.addFlashAttribute("success", "Đã xóa loại thiết bị.");
		} catch (ApiException exception) {
			redirectAttributes.addFlashAttribute("error", exception.getMessage());
		}
		return "redirect:/catalog/device-types";
	}

	@ModelAttribute("principalName")
	String principalName(Principal principal) {
		return principal == null ? "" : principal.getName();
	}

	private String roomForm(Model model, boolean editing) {
		model.addAttribute("editing", editing);
		model.addAttribute("groups", catalogService.roomGroups());
		model.addAttribute("statuses", PhongTrangThai.values());
		return "catalog/room-form";
	}

	private String deviceForm(Model model, boolean editing) {
		model.addAttribute("editing", editing);
		model.addAttribute("types", catalogService.deviceTypes());
		model.addAttribute("rooms", catalogService.selectableRooms());
		model.addAttribute("statuses", ThietBiTrangThai.values());
		return "catalog/device-form";
	}

	private String roomGroups(Model model) {
		model.addAttribute("groups", catalogService.roomGroups());
		return "catalog/room-groups";
	}

	private String deviceTypes(Model model) {
		model.addAttribute("types", catalogService.deviceTypes());
		return "catalog/device-types";
	}

	private boolean isManager(Authentication authentication) {
		return authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_CBQL".equals(authority.getAuthority()));
	}
}
