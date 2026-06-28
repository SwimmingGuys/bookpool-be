package kr.co.bookpool.common.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

// 운영에서는 Swagger 를 노출하지 않는다(springdoc 도 prod 에서 비활성화).
// → prod 에서는 이 빈이 로드되지 않으므로 swagger.server-url 도 불필요.
@Profile("!prod")
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
