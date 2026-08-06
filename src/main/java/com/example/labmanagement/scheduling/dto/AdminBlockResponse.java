package com.example.labmanagement.scheduling.dto;

import com.example.labmanagement.catalog.domain.LoaiTaiNguyen;
import com.example.labmanagement.scheduling.domain.LichChanTrangThai;
import java.time.LocalDate;

public record AdminBlockResponse(Long id, LoaiTaiNguyen resourceType, String resourceId, String resourceName,
		LocalDate startDate, LocalDate endDate, Integer dayOfWeek, String dayLabel, Integer periodId, String periodName,
		String reason, LichChanTrangThai status, String creatorId, String creatorName) {
}
