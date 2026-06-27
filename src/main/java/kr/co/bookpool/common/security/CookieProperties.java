package kr.co.bookpool.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 리프레시 토큰 쿠키 속성. 배포 환경에 따라 달라진다.
 * - 로컬(http, 동일 사이트): secure=false, sameSite=Lax
 * - 운영(https, 교차 사이트): secure=true, sameSite=None  (None은 Secure 필수)
 */
@ConfigurationProperties(prefix = "app.auth.cookie")
public record CookieProperties(
	boolean secure,
	String sameSite
) {
}