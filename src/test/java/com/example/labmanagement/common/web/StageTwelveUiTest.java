package com.example.labmanagement.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class StageTwelveUiTest {

	private static final List<String> AUTHENTICATED_TEMPLATES = List.of("catalog/device-form.html",
			"catalog/device-types.html", "catalog/devices.html", "catalog/room-form.html", "catalog/room-groups.html",
			"catalog/rooms.html", "incident/detail.html", "incident/form.html", "incident/list.html",
			"maintenance/detail.html", "maintenance/form.html", "maintenance/list.html", "notification/list.html",
			"registration/detail.html", "registration/form.html", "registration/list.html", "reporting/dashboard.html",
			"identity/user-approvals.html", "scheduling/admin-blocks.html", "scheduling/availability.html",
			"scheduling/calendar.html", "usage/detail.html", "usage/list.html", "home.html", "profile.html");

	@Test
	void authenticatedPagesUseSharedRoleAwareNavigation() throws IOException {
		for (String template : AUTHENTICATED_TEMPLATES) {
			assertThat(read("templates/" + template)).as(template)
					.contains("th:replace=\"~{fragments/navigation :: header}\"");
		}
		String navigation = read("templates/fragments/navigation.html");
		assertThat(navigation).contains("th:if=\"${navManager}\"").contains("/dashboard").contains("/admin/users")
				.contains("/maintenances").contains("/admin-blocks").contains("data-sidebar-toggle")
				.contains("data-sidebar-dismiss");
	}

	@Test
	void sharedUiProvidesResponsiveAccessibleInteractionStates() throws IOException {
		String css = read("static/css/catalog.css");
		String javascript = read("static/js/app-ui.js");
		assertThat(css).contains("@media (max-width: 48rem)", "@media (max-width: 36rem)",
				"@media (prefers-reduced-motion: reduce)", ".confirm-dialog", ".button-loading",
				"--sidebar-collapsed-width", ".app-header:hover", "body.nav-open .app-header");
		assertThat(javascript).contains("aria-invalid", "aria-busy", "showModal", "requestSubmit", "nav-open", "Escape",
				"aria-expanded");
	}

	@Test
	void stateChangingFormsUseAccessibleConfirmationDialog() throws IOException {
		for (String template : AUTHENTICATED_TEMPLATES) {
			assertThat(read("templates/" + template)).as(template).doesNotContain("onsubmit=\"return confirm");
		}
		assertThat(read("templates/registration/detail.html")).contains("data-confirm=");
		assertThat(read("templates/identity/user-approvals.html")).contains("data-confirm=");
		assertThat(read("templates/usage/detail.html")).contains("data-confirm=");
	}

	private String read(String classpathLocation) throws IOException {
		return new ClassPathResource(classpathLocation).getContentAsString(StandardCharsets.UTF_8);
	}
}
