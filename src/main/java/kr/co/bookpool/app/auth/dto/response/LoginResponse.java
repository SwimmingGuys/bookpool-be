package kr.co.bookpool.app.auth.dto.response;

public record LoginResponse(
	String accessToken,
	String tokenType
) {

	private static final String BEARER = "Bearer";

	public static LoginResponse of(String accessToken) {
		return new LoginResponse(accessToken, BEARER);
	}
}
