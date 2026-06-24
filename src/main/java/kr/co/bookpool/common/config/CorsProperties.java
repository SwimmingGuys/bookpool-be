package kr.co.bookpool.common.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 쿠키(자격 증명)를 주고받으려면 allowCredentials=true가 필요하고,
 * 이 경우 허용 오리진에 와일드카드("*")를 쓸 수 없으므로 명시적으로 나열한다.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
	List<String> allowedOrigins
) {
}