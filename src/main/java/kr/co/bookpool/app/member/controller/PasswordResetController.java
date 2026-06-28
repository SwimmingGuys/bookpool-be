package kr.co.bookpool.app.member.controller;

import static org.springframework.http.HttpStatus.*;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.co.bookpool.app.member.controller.docs.PasswordResetControllerDocs;
import kr.co.bookpool.app.member.dto.request.EmailCodeRequest;
import kr.co.bookpool.app.member.dto.request.EmailVerifyRequest;
import kr.co.bookpool.app.member.dto.request.ResetPasswordRequest;
import kr.co.bookpool.app.member.service.EmailVerificationService;
import kr.co.bookpool.app.member.service.MemberService;
import kr.co.bookpool.common.response.ApiResult;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/password")
public class PasswordResetController implements PasswordResetControllerDocs {

	private final EmailVerificationService emailVerificationService;
	private final MemberService memberService;

	@Override
	@PostMapping("/email/code")
	@ResponseStatus(OK)
	public ApiResult<Void> sendCode(@Valid @RequestBody EmailCodeRequest request) {
		emailVerificationService.sendResetCode(request.email());
		return ApiResult.<Void>success(null);
	}

	@Override
	@PostMapping("/email/verify")
	@ResponseStatus(OK)
	public ApiResult<Void> verify(@Valid @RequestBody EmailVerifyRequest request) {
		emailVerificationService.verify(request.email(), request.code());
		return ApiResult.<Void>success(null);
	}

	@Override
	@PostMapping("/reset")
	@ResponseStatus(OK)
	public ApiResult<Void> reset(@Valid @RequestBody ResetPasswordRequest request) {
		memberService.resetPassword(request);
		return ApiResult.<Void>success(null);
	}
}
