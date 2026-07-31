package com.example.labmanagement.registration.application;

import com.example.labmanagement.registration.domain.HanhDongXuLyPhieu;
import java.time.OffsetDateTime;

public record RegistrationHistoryResponse(HanhDongXuLyPhieu action, String handlerId, String handlerName, String reason,
		OffsetDateTime occurredAt) {
}
