package com.example.labmanagement;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.labmanagement.catalog.service.CatalogService;
import com.example.labmanagement.identity.repository.NguoiDungRepository;
import com.example.labmanagement.identity.repository.VaiTroRepository;
import com.example.labmanagement.incident.service.IncidentService;
import com.example.labmanagement.maintenance.service.MaintenanceService;
import com.example.labmanagement.notification.repository.NotificationProjectionRepository;
import com.example.labmanagement.notification.service.NotificationService;
import com.example.labmanagement.registration.service.ApprovalService;
import com.example.labmanagement.registration.service.RegistrationService;
import com.example.labmanagement.reporting.repository.DashboardQueryRepository;
import com.example.labmanagement.reporting.service.DashboardService;
import com.example.labmanagement.scheduling.service.AdminBlockService;
import com.example.labmanagement.scheduling.service.SchedulingService;
import com.example.labmanagement.usage.service.SessionGenerationService;
import com.example.labmanagement.usage.service.UsageSessionService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = LabManagementApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"spring.autoconfigure.exclude=" + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
				+ "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
				+ "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
		"spring.data.jpa.repositories.enabled=false"})
@Import(ReadinessEndpointTest.HealthTestConfiguration.class)
class ReadinessEndpointTest {

	@LocalServerPort
	private int port;

	@MockitoBean
	private NguoiDungRepository nguoiDungRepository;

	@MockitoBean
	private VaiTroRepository vaiTroRepository;

	@MockitoBean
	private CatalogService catalogService;

	@MockitoBean
	private SchedulingService schedulingService;

	@MockitoBean
	private RegistrationService registrationService;

	@MockitoBean
	private ApprovalService approvalService;

	@MockitoBean
	private AdminBlockService adminBlockService;

	@MockitoBean
	private SessionGenerationService sessionGenerationService;

	@MockitoBean
	private UsageSessionService usageSessionService;

	@MockitoBean
	private IncidentService incidentService;

	@MockitoBean
	private MaintenanceService maintenanceService;

	@MockitoBean
	private NotificationService notificationService;

	@MockitoBean
	private DashboardService dashboardService;

	@MockitoBean
	private NotificationProjectionRepository notificationProjectionRepository;

	@MockitoBean
	private DashboardQueryRepository dashboardQueryRepository;

	@Test
	void readinessIsPublicAndReturnsHttp200() throws Exception {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:" + port + "/actuator/health/readiness")).GET().build();

		HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"status\":\"UP\"");
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class HealthTestConfiguration {

		@Bean
		HealthIndicator dbHealthIndicator() {
			return () -> Health.up().build();
		}
	}
}
