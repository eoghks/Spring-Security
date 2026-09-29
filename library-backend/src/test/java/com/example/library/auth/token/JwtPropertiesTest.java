package com.example.library.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 서명 키 fail-fast 검증.
 * 실제 application.yml / application-default.yml 을 읽는 최소 컨텍스트(JwtProperties 만 바인딩)로 프로필별 동작을 확인한다.
 */
class JwtPropertiesTest {

	/** 디코드 32바이트짜리 운영용 예시 키 */
	private static final String STRONG_SECRET = Base64.getEncoder().encodeToString(new byte[32]);

	/** 디코드 16바이트 — HS256 에 부족 */
	private static final String SHORT_SECRET = Base64.getEncoder().encodeToString(new byte[16]);

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(JwtProperties.class)
	static class JwtOnlyConfig {
	}

	@Test
	@DisplayName("기본(H2) 개발 프로필은 JWT_SECRET 없이도 개발용 키로 기동한다")
	void defaultProfileUsesDevSecret() {
		assumeTrue(System.getenv("JWT_SECRET") == null, "JWT_SECRET 환경 변수가 설정된 PC 에서는 건너뛴다");
		try (ConfigurableApplicationContext context = start()) {
			JwtProperties properties = context.getBean(JwtProperties.class);
			assertThat(properties.secret()).isEqualTo(JwtProperties.DEV_ONLY_SECRET);
			assertThat(properties.devSecretAllowed()).isTrue();
		}
	}

	@Test
	@DisplayName("postgres 프로필에서 JWT_SECRET 을 주지 않으면 기동에 실패한다")
	void otherProfileWithoutSecretFails() {
		assumeTrue(System.getenv("JWT_SECRET") == null, "JWT_SECRET 환경 변수가 설정된 PC 에서는 건너뛴다");
		assertThatThrownBy(() -> start("--spring.profiles.active=postgres"))
				.rootCause().hasMessageContaining("JWT_SECRET");
	}

	@Test
	@DisplayName("postgres 프로필에서 개발용 키를 주면 기동에 실패한다")
	void otherProfileWithDevSecretFails() {
		assertThatThrownBy(() -> start("--spring.profiles.active=postgres",
				"--app.jwt.secret=" + JwtProperties.DEV_ONLY_SECRET))
				.rootCause().hasMessageContaining("개발용 JWT 서명 키");
	}

	@Test
	@DisplayName("postgres 프로필에서 32바이트 미만 키를 주면 기동에 실패한다")
	void otherProfileWithShortSecretFails() {
		assertThatThrownBy(() -> start("--spring.profiles.active=postgres", "--app.jwt.secret=" + SHORT_SECRET))
				.rootCause().hasMessageContaining("32바이트");
	}

	@Test
	@DisplayName("postgres 프로필에서 충분히 긴 키를 주면 정상 기동하고 개발용 키 허용은 꺼져 있다")
	void otherProfileWithStrongSecretStarts() {
		try (ConfigurableApplicationContext context = start("--spring.profiles.active=postgres",
				"--app.jwt.secret=" + STRONG_SECRET)) {
			JwtProperties properties = context.getBean(JwtProperties.class);
			assertThat(properties.secret()).isEqualTo(STRONG_SECRET);
			assertThat(properties.devSecretAllowed()).isFalse();
		}
	}

	@Test
	@DisplayName("레코드 생성 단계에서도 빈 키·짧은 키·허용되지 않은 개발용 키를 거부한다")
	void constructorValidation() {
		assertThatThrownBy(() -> properties(" ", false)).hasMessageContaining("JWT_SECRET");
		assertThatThrownBy(() -> properties(SHORT_SECRET, true)).hasMessageContaining("32바이트");
		assertThatThrownBy(() -> properties(JwtProperties.DEV_ONLY_SECRET, false)).hasMessageContaining("개발용");
		assertThat(properties(JwtProperties.DEV_ONLY_SECRET, true).secret()).isEqualTo(JwtProperties.DEV_ONLY_SECRET);
	}

	private JwtProperties properties(String secret, boolean devSecretAllowed) {
		return new JwtProperties(secret, "issuer", Duration.ofMinutes(15), Duration.ofDays(7), devSecretAllowed);
	}

	private ConfigurableApplicationContext start(String... args) {
		return new SpringApplicationBuilder(JwtOnlyConfig.class)
				.web(WebApplicationType.NONE)
				.bannerMode(Banner.Mode.OFF)
				.logStartupInfo(false)
				.run(args);
	}
}
