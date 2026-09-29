package com.example.library.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.library.LibraryApplication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Swagger(API 문서)는 기본(H2) 개발 프로필에서만 열리고, 그 밖 프로필에서는 꺼지는지 실제 서버로 확인한다.
 * postgres 프로필 설정 파일을 그대로 읽되 DB 만 H2 인메모리로 바꿔 띄운다(기본 프로필 동작은 OpenApiIntegrationTest 가 검증).
 */
class SwaggerProfileTest {

	@Test
	@DisplayName("postgres 프로필로 기동하면 /v3/api-docs 와 Swagger UI 가 404 다")
	void swaggerIsDisabledOutsideDefaultProfile() throws Exception {
		try (ConfigurableApplicationContext context = startWithPostgresProfile()) {
			int port = ((WebServerApplicationContext) context).getWebServer().getPort();

			assertThat(status(port, "/v3/api-docs")).isEqualTo(404);
			assertThat(status(port, "/swagger-ui/index.html")).isEqualTo(404);
		}
	}

	private ConfigurableApplicationContext startWithPostgresProfile() {
		return new SpringApplicationBuilder(LibraryApplication.class)
				.bannerMode(Banner.Mode.OFF)
				.run("--spring.profiles.active=postgres",
						"--server.port=0",
						"--spring.datasource.url=jdbc:h2:mem:swagger-" + UUID.randomUUID()
								+ ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
						"--spring.datasource.username=sa",
						"--spring.datasource.password=",
						"--spring.sql.init.mode=always",
						"--app.jwt.secret=" + Base64.getEncoder().encodeToString(new byte[32]),
						"--app.hazelcast.cluster-name=swagger-test-" + UUID.randomUUID(),
						"--app.hazelcast.port=5931");
	}

	private int status(int port, String path) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build();
		return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
	}
}
