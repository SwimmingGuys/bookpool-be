package kr.co.bookpool.app.auth.controller;

import static kr.co.bookpool.common.security.RefreshCookieFactory.*;
import static org.springframework.http.HttpStatus.*;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import kr.co.bookpool.app.auth.controller.docs.AuthControllerDocs;
import kr.co.bookpool.app.auth.dto.AuthTokens;
import kr.co.bookpool.app.auth.dto.request.LoginRequest;
import kr.co.bookpool.app.auth.dto.response.LoginResponse;
import kr.co.bookpool.app.auth.service.AuthService;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.security.RefreshCookieFactory;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

	private final AuthService authService;
	private final RefreshCookieFactory refreshCookieFactory;

	@Override
	@PostMapping("/api/login")
	@ResponseStatus(OK)
	public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
		AuthTokens tokens = authService.login(request);
		writeCookie(response, refreshCookieFactory.create(tokens.refreshToken()));
		return ApiResult.success(LoginResponse.of(tokens.accessToken()));
	}

	@Override
	@PostMapping("/api/reissue")
	@ResponseStatus(OK)
	public ApiResult<LoginResponse> reissue(
		@CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
		HttpServletResponse response) {
		AuthTokens tokens = authService.reissue(refreshToken);
		writeCookie(response, refreshCookieFactory.create(tokens.refreshToken()));
		return ApiResult.success(LoginResponse.of(tokens.accessToken()));
	}

	@Override
	@PostMapping("/api/logout")
	@ResponseStatus(OK)
	public ApiResult<Void> logout(
		@CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
		HttpServletResponse response) {
		authService.logout(refreshToken);
		writeCookie(response, refreshCookieFactory.expire());
		return ApiResult.<Void>success(null);
	}

	private void writeCookie(HttpServletResponse response, ResponseCookie cookie) {
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}
}