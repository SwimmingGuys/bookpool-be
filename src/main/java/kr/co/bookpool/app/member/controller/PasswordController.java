package kr.co.bookpool.app.member.controller;

import static org.springframework.http.HttpStatus.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.co.bookpool.app.member.controller.docs.PasswordControllerDocs;
import kr.co.bookpool.app.member.dto.request.EmailCodeRequest;
import kr.co.bookpool.app.member.dto.request.EmailVerifyRequest;
import kr.co.bookpool.app.member.dto.request.PasswordChangeRequest;
import kr.co.bookpool.app.member.dto.request.PasswordResetRequest;
import kr.co.bookpool.app.member.service.EmailVerificationService;
import kr.co.bookpool.app.member.service.PasswordService;
import kr.co.bookpool.common.response.ApiResult;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PasswordController implements PasswordControllerDocs {

	private final PasswordService passwordService;
	private final EmailVerificationService emailVerificationService;

	@Override
	@PatchMapping("/api/me/password")
	@ResponseStatus(OK)
	public ApiResult<Void> changePassword(
		@AuthenticationPrincipal Long memberId,
		@Valid @RequestBody PasswordChangeRequest request
	) {
		passwordService.changePassword(memberId, request.currentPassword(), request.newPassword());
		return ApiResult.<Void>success(null);
	}

	@Override
	@PostMapping("/api/password/email/code")
	@ResponseStatus(OK)
	public ApiResult<Void> sendResetCode(@Valid @RequestBody EmailCodeRequest request) {
		emailVerificationService.sendCodeForPasswordReset(request.email());
		return ApiResult.<Void>success(null);
	}

	@Override
	@PostMapping("/api/password/email/verify")
	@ResponseStatus(OK)
	public ApiResult<Void> verifyResetCode(@Valid @RequestBody EmailVerifyRequest request) {
		emailVerificationService.verify(request.email(), request.code());
		return ApiResult.<Void>success(null);
	}

	@Override
	@PostMapping("/api/password/reset")
	@ResponseStatus(OK)
	public ApiResult<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
		passwordService.resetPassword(request.email(), request.newPassword());
		return ApiResult.<Void>success(null);
	}
}
