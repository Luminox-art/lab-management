package com.example.labmanagement.catalog.web;

import com.example.labmanagement.catalog.application.CatalogPageMeta;
import com.example.labmanagement.catalog.application.CatalogService;
import com.example.labmanagement.catalog.application.DeviceCreateRequest;
import com.example.labmanagement.catalog.application.DeviceResponse;
import com.example.labmanagement.catalog.application.DeviceUpdateRequest;
import com.example.labmanagement.catalog.domain.ThietBiTrangThai;
import com.example.labmanagement.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API-12..14, FR-06/FR-07, UC-05/UC-18. */
@RestController
@RequestMapping("/api/v1/devices")
public class DeviceRestController {

	private final CatalogService catalogService;

	public DeviceRestController(CatalogService catalogService) {
		this.catalogService = catalogService;
	}

	@GetMapping
	ApiResponse<List<DeviceResponse>> devices(@RequestParam(required = false) String type,
			@RequestParam(required = false) String room, @RequestParam(required = false) ThietBiTrangThai status,
			@RequestParam(required = false) String keyword, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "id,asc") String sort) {
		Page<DeviceResponse> result = catalogService.searchDevices(type, room, status, keyword, page, size, sort);
		return ApiResponse.of(result.getContent(), new CatalogPageMeta(result.getNumber(), result.getSize(),
				result.getTotalElements(), result.getTotalPages(), catalogService.normalizeDeviceSort(sort)));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ApiResponse<DeviceResponse> create(@Valid @RequestBody DeviceCreateRequest request) {
		return ApiResponse.of(catalogService.createDevice(request));
	}

	@PutMapping("/{id}")
	ApiResponse<DeviceResponse> update(@PathVariable String id, @Valid @RequestBody DeviceUpdateRequest request) {
		return ApiResponse.of(catalogService.updateDevice(id, request));
	}
}
