package com.example.labmanagement;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.labmanagement.catalog.application.CatalogService;
import com.example.labmanagement.identity.persistence.NguoiDungRepository;
import com.example.labmanagement.identity.persistence.VaiTroRepository;
import com.example.labmanagement.registration.application.ApprovalService;
import com.example.labmanagement.registration.application.RegistrationService;
import com.example.labmanagement.scheduling.application.AdminBlockService;
import com.example.labmanagement.scheduling.application.SchedulingService;
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
