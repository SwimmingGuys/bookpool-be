package kr.co.bookpool.app.auth.dto;

/**
 * 서비스 계층에서 컨트롤러로 전달하는 토큰 묶음(내부 DTO).
 * accessToken은 응답 바디로, refreshToken은 httpOnly 쿠키로 내려간다.
 */
public record AuthTokens(
	String accessToken,
	String refreshToken
) {
}