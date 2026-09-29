package com.example.library.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI(/swagger-ui.html) 설정. Bearer JWT 와 X-API-KEY 두 인증 방식을 등록한다.
 */
@Configuration
public class OpenApiConfig {

	private static final String BEARER = "bearerAuth";
	private static final String API_KEY = "apiKey";

	@Bean
	public OpenAPI libraryOpenApi() {
		return new OpenAPI()
				.info(new Info().title("도서관 대출 시스템 API").version("v1")
						.description("URL 기반 인가(메뉴·액션·URL 모델), JWT, API Key 예제"))
				.components(new Components()
						.addSecuritySchemes(BEARER, new SecurityScheme().type(SecurityScheme.Type.HTTP)
								.scheme("bearer").bearerFormat("JWT"))
						.addSecuritySchemes(API_KEY, new SecurityScheme().type(SecurityScheme.Type.APIKEY)
								.in(SecurityScheme.In.HEADER).name("X-API-KEY")))
				.addSecurityItem(new SecurityRequirement().addList(BEARER))
				.addSecurityItem(new SecurityRequirement().addList(API_KEY));
	}
}
