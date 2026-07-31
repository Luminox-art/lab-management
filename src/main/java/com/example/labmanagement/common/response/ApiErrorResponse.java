package com.example.labmanagement.common.response;

import java.util.List;

public record ApiErrorResponse(String code, String message, List<FieldErrorResponse> fieldErrors, String traceId) {
}
