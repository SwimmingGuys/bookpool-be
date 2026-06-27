package kr.co.bookpool.common.security;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 리프레시 토큰을 담는 httpOnly 쿠키를 생성한다.
 * httpOnly라 JS에서 읽을 수 없어 XSS로 인한 탈취를 막는다.
 */
@Component
@RequiredArgsConstructor
public class RefreshCookieFactory {

	public static final String REFRESH_COOKIE_NAME = "refreshToken";

	private final CookieProperties cookieProperties;
	private final JwtProvider jwtProvider;

	public ResponseCookie create(String refreshToken) {
		return baseBuilder(refreshToken)
			.maxAge(Duration.ofMillis(jwtProvider.getRefreshTokenValidityMs()))
			.build();
	}

	/** 로그아웃 시 즉시 만료시키는 빈 쿠키. */
	public ResponseCookie expire() {
		return baseBuilder("")
			.maxAge(0)
			.build();
	}

	private ResponseCookie.ResponseCookieBuilder baseBuilder(String value) {
		return ResponseCookie.from(REFRESH_COOKIE_NAME, value)
			.httpOnly(true)
			.secure(cookieProperties.secure())
			.sameSite(cookieProperties.sameSite())
			.path("/");
	}
}