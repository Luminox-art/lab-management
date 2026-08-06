package com.example.labmanagement.catalog.controller;

import com.example.labmanagement.catalog.domain.PhongTrangThai;
import com.example.labmanagement.catalog.dto.CatalogPageMeta;
import com.example.labmanagement.catalog.dto.RoomCreateRequest;
import com.example.labmanagement.catalog.dto.RoomResponse;
import com.example.labmanagement.catalog.dto.RoomUpdateRequest;
import com.example.labmanagement.catalog.service.CatalogService;
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

/** API-09..11, FR-05/FR-07, UC-05/UC-17. */
@RestController
@RequestMapping("/api/v1/rooms")
public class RoomRestController {

	private final CatalogService catalogService;

	public RoomRestController(CatalogService catalogService) {
		this.catalogService = catalogService;
	}

	@GetMapping
	ApiResponse<List<RoomResponse>> rooms(@RequestParam(required = false) String group,
			@RequestParam(required = false) PhongTrangThai status, @RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "id,asc") String sort) {
		Page<RoomResponse> result = catalogService.searchRooms(group, status, keyword, page, size, sort);
		return ApiResponse.of(result.getContent(), new CatalogPageMeta(result.getNumber(), result.getSize(),
				result.getTotalElements(), result.getTotalPages(), catalogService.normalizeRoomSort(sort)));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ApiResponse<RoomResponse> create(@Valid @RequestBody RoomCreateRequest request) {
		return ApiResponse.of(catalogService.createRoom(request));
	}

	@PutMapping("/{id}")
	ApiResponse<RoomResponse> update(@PathVariable String id, @Valid @RequestBody RoomUpdateRequest request) {
		return ApiResponse.of(catalogService.updateRoom(id, request));
	}
}
