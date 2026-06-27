package kr.co.bookpool.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
	String secret,
	long accessTokenValidityMs,
	long refreshTokenValidityMs
) {
}
