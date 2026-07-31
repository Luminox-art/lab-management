package com.example.labmanagement.common.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.labmanagement.common.trace.TraceIdFilter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
				.setControllerAdvice(new GlobalExceptionHandler()).addFilter(new TraceIdFilter()).build();
	}

	@Test
	void validationErrorUsesCommonFormatAndTraceId() throws Exception {
		mockMvc.perform(post("/validate").header(TraceIdFilter.TRACE_ID_HEADER, "test-trace-123")
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}")).andExpect(status().isBadRequest())
				.andExpect(header().string(TraceIdFilter.TRACE_ID_HEADER, "test-trace-123"))
				.andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
				.andExpect(jsonPath("$.traceId").value("test-trace-123"));
	}

	@Test
	void apiExceptionKeepsDeclaredStatusAndHidesStackTrace() throws Exception {
		mockMvc.perform(get("/conflict")).andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_CONFLICT.name()))
				.andExpect(jsonPath("$.message").value("Tài nguyên đang được sử dụng."))
				.andExpect(jsonPath("$.traceId").isNotEmpty()).andExpect(jsonPath("$.stackTrace").doesNotExist());
	}

	@RestController
	static class TestController {

		@PostMapping("/validate")
		void validate(@Valid @RequestBody TestRequest request) {
		}

		@GetMapping("/conflict")
		void conflict() {
			throw new ApiException(ErrorCode.RESOURCE_CONFLICT, HttpStatus.CONFLICT, "Tài nguyên đang được sử dụng.");
		}
	}

	record TestRequest(@NotBlank String name) {
	}
}
