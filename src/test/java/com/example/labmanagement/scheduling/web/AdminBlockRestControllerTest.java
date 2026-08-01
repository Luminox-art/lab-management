package com.example.labmanagement.scheduling.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.labmanagement.common.error.GlobalExceptionHandler;
import com.example.labmanagement.scheduling.application.AdminBlockCreationResponse;
import com.example.labmanagement.scheduling.application.AdminBlockRequest;
import com.example.labmanagement.scheduling.application.AdminBlockService;
import com.example.labmanagement.security.SecurityConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@ContextConfiguration(classes = {AdminBlockRestController.class, SecurityConfiguration.class,
		GlobalExceptionHandler.class})
class AdminBlockRestControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AdminBlockService adminBlockService;

	@Test
	void createRequiresManagerAndCsrf() throws Exception {
		when(adminBlockService.create(any(), any())).thenReturn(new AdminBlockCreationResponse(List.of()));

		mockMvc.perform(
				post("/api/v1/admin-blocks").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(validJson()))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/v1/admin-blocks").with(user("gv001@lab.local").roles("GV")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(validJson())).andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/admin-blocks").with(user("cb001@lab.local").roles("CBQL"))
				.contentType(MediaType.APPLICATION_JSON).content(validJson())).andExpect(status().isForbidden());

		mockMvc.perform(post("/api/v1/admin-blocks").with(user("cb001@lab.local").roles("CBQL")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(validJson())).andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.blocks").isArray());
		verify(adminBlockService).create(org.mockito.ArgumentMatchers.eq("cb001@lab.local"),
				any(AdminBlockRequest.class));
	}

	@Test
	void invalidDayIsRejectedBeforeServiceCall() throws Exception {
		mockMvc.perform(post("/api/v1/admin-blocks").with(user("cb001@lab.local").roles("CBQL")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(validJson().replace("\"dayOfWeek\":2", "\"dayOfWeek\":9"))).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		verify(adminBlockService, never()).create(any(), any());
	}

	@Test
	void deleteCancelsLogicallyAndReturnsNoContent() throws Exception {
		mockMvc.perform(delete("/api/v1/admin-blocks/15").with(user("cb001@lab.local").roles("CBQL")).with(csrf()))
				.andExpect(status().isNoContent());
		verify(adminBlockService).cancel("cb001@lab.local", 15L);
	}

	private String validJson() {
		return """
				{"roomId":"P0601","startDate":"2035-01-01","endDate":"2035-01-31",
				 "dayOfWeek":2,"periodIds":[1,2],"reason":"Bảo trì định kỳ"}
				""";
	}
}
