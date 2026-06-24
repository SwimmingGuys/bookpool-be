package kr.co.bookpool.app.member.controller;

import static org.springframework.http.HttpStatus.*;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.co.bookpool.app.member.controller.docs.EmailVerificationControllerDocs;
import kr.co.bookpool.app.member.dto.request.EmailCodeRequest;
import kr.co.bookpool.app.member.dto.request.EmailVerifyRequest;
import kr.co.bookpool.app.member.service.EmailVerificationService;
import kr.co.bookpool.common.response.ApiResult;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class EmailVerificationController implements EmailVerificationControllerDocs {

	private final EmailVerificationService emailVerificationService;

	@Override
	@PostMapping("/api/signup/email/code")
	@ResponseStatus(OK)
	public ApiResult<Void> sendCode(@Valid @RequestBody EmailCodeRequest request) {
		emailVerificationService.sendCode(request.email());
		return ApiResult.<Void>success(null);
	}

	@Override
	@PostMapping("/api/signup/email/verify")
	@ResponseStatus(OK)
	public ApiResult<Void> verify(@Valid @RequestBody EmailVerifyRequest request) {
		emailVerificationService.verify(request.email(), request.code());
		return ApiResult.<Void>success(null);
	}
}
