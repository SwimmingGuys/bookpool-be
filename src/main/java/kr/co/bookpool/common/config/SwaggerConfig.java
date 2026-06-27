package kr.co.bookpool.common.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class SwaggerConfig {

	private static final String BEARER_SCHEME = "bearerAuth";

	@Value("${swagger.server-url}")
	private String SERVER_URL;

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
			.servers(List.of(
				new Server().url(SERVER_URL)
			))
			.components(new Components()
				.addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT")))
			.info(info);
	}

	Info info = new Info()
		.title("BOOKPOOL API")
		.version("0.0.1")
		.description("<h3>BOOKPOOL :)</h3>");
}
