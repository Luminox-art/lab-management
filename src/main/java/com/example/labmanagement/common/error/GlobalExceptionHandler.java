package com.example.labmanagement.common.error;

import com.example.labmanagement.common.response.ApiErrorResponse;
import com.example.labmanagement.common.response.FieldErrorResponse;
import com.example.labmanagement.common.trace.TraceContext;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
		List<FieldErrorResponse> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
				.map(this::toFieldErrorResponse).toList();
		return error(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Dữ liệu đầu vào không hợp lệ.", fieldErrors);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
		List<FieldErrorResponse> fieldErrors = exception.getConstraintViolations().stream().map(
				violation -> new FieldErrorResponse(violation.getPropertyPath().toString(), violation.getMessage()))
				.toList();
		return error(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Dữ liệu đầu vào không hợp lệ.", fieldErrors);
	}

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ApiErrorResponse> handleApiException(ApiException exception) {
		return error(exception.getStatus(), exception.getCode(), exception.getMessage(), List.of());
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception) {
		LOGGER.error("Lỗi không dự kiến", exception);
		return error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "Hệ thống không thể xử lý yêu cầu.",
				List.of());
	}

	private FieldErrorResponse toFieldErrorResponse(FieldError fieldError) {
		return new FieldErrorResponse(fieldError.getField(), fieldError.getDefaultMessage());
	}

	private ResponseEntity<ApiErrorResponse> error(HttpStatus status, ErrorCode code, String message,
			List<FieldErrorResponse> fieldErrors) {
		ApiErrorResponse body = new ApiErrorResponse(code.name(), message, fieldErrors, TraceContext.currentTraceId());
		return ResponseEntity.status(status).body(body);
	}
}
