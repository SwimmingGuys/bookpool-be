package kr.co.bookpool.app.auth.controller;

import static org.springframework.http.HttpStatus.*;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.co.bookpool.app.auth.controller.docs.AuthControllerDocs;
import kr.co.bookpool.app.auth.dto.request.LoginRequest;
import kr.co.bookpool.app.auth.dto.response.LoginResponse;
import kr.co.bookpool.app.auth.service.AuthService;
import kr.co.bookpool.common.response.ApiResult;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

	private final AuthService authService;

	@Override
	@PostMapping("/api/login")
	@ResponseStatus(OK)
	public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResult.success(authService.login(request));
	}
}