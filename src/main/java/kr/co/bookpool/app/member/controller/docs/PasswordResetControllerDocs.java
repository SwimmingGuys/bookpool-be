package kr.co.bookpool.app.member.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.bookpool.app.member.dto.request.EmailCodeRequest;
import kr.co.bookpool.app.member.dto.request.EmailVerifyRequest;
import kr.co.bookpool.app.member.dto.request.ResetPasswordRequest;
import kr.co.bookpool.common.response.ApiResult;

@Tag(name = "Password Reset", description = "비밀번호 재설정 API (비로그인)")
public interface PasswordResetControllerDocs {

	@Operation(summary = "재설정 인증 코드 발송", description = "가입된 이메일로 비밀번호 재설정용 인증 코드를 발송합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "발송 성공"),
		@ApiResponse(
			responseCode = "404", description = "가입되지 않은 이메일",
			content = @Content(examples = @ExampleObject(value = """
				{ "success": false, "code": "M002", "message": "회원을 찾을 수 없습니다." }""")))
	})
	ApiResult<Void> sendCode(EmailCodeRequest request);

	@Operation(summary = "재설정 인증 코드 확인", description = "이메일로 받은 인증 코드를 확인합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "확인 성공"),
		@ApiResponse(
			responseCode = "400", description = "코드 불일치 또는 만료",
			content = @Content(examples = @ExampleObject(value = """
				{ "success": false, "code": "M004", "message": "인증 코드가 올바르지 않거나 만료되었습니다." }""")))
	})
	ApiResult<Void> verify(EmailVerifyRequest request);

	@Operation(summary = "비밀번호 재설정", description = "이메일 인증을 완료한 계정의 비밀번호를 새 비밀번호로 변경합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "재설정 성공"),
		@ApiResponse(
			responseCode = "400", description = "이메일 인증 미완료 또는 입력값 검증 실패",
			content = @Content(examples = @ExampleObject(value = """
				{ "success": false, "code": "M003", "message": "이메일 인증이 완료되지 않았습니다." }""")))
	})
	ApiResult<Void> reset(ResetPasswordRequest request);
}
