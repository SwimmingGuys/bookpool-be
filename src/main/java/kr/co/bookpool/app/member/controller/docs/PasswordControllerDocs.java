package kr.co.bookpool.app.member.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.bookpool.app.member.dto.request.EmailCodeRequest;
import kr.co.bookpool.app.member.dto.request.EmailVerifyRequest;
import kr.co.bookpool.app.member.dto.request.PasswordChangeRequest;
import kr.co.bookpool.app.member.dto.request.PasswordResetRequest;
import kr.co.bookpool.common.response.ApiResult;

@Tag(name = "Password", description = "비밀번호 변경/재설정 API")
public interface PasswordControllerDocs {

	@Operation(summary = "비밀번호 변경 (로그인 상태)",
		description = "현재 비밀번호를 확인한 뒤 새 비밀번호로 변경합니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@RequestBody(content = @Content(examples = @ExampleObject(value = """
		{
		  "currentPassword": "password1234",
		  "newPassword": "newPassword1234"
		}""")))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "변경 성공"),
		@ApiResponse(
			responseCode = "400", description = "현재 비밀번호 불일치",
			content = @Content(examples = @ExampleObject(value = """
				{
				  "success": false,
				  "code": "M005",
				  "message": "현재 비밀번호가 일치하지 않습니다."
				}"""))),
		@ApiResponse(
			responseCode = "401", description = "인증 실패 (토큰 없음/유효하지 않음)",
			content = @Content(examples = @ExampleObject(value = """
				{
				  "success": false,
				  "code": "A002",
				  "message": "유효하지 않은 인증 정보입니다."
				}""")))
	})
	ApiResult<Void> changePassword(Long memberId, PasswordChangeRequest request);

	@Operation(summary = "비밀번호 재설정 코드 발송",
		description = "가입된 이메일로 6자리 인증 코드를 발송합니다. 가입되지 않은 이메일이면 404를 반환합니다.")
	@RequestBody(content = @Content(examples = @ExampleObject(value = """
		{
		  "email": "test@bookpool.kr"
		}""")))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "발송 성공"),
		@ApiResponse(
			responseCode = "404", description = "가입되지 않은 이메일",
			content = @Content(examples = @ExampleObject(value = """
				{
				  "success": false,
				  "code": "M002",
				  "message": "회원을 찾을 수 없습니다."
				}""")))
	})
	ApiResult<Void> sendResetCode(EmailCodeRequest request);

	@Operation(summary = "비밀번호 재설정 코드 확인",
		description = "발송된 코드를 검증합니다. 성공하면 해당 이메일은 일정 시간 동안 '인증 완료' 상태가 되어 비밀번호 재설정이 가능합니다.")
	@RequestBody(content = @Content(examples = @ExampleObject(value = """
		{
		  "email": "test@bookpool.kr",
		  "code": "123456"
		}""")))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "인증 성공"),
		@ApiResponse(
			responseCode = "400", description = "코드가 올바르지 않거나 만료됨",
			content = @Content(examples = @ExampleObject(value = """
				{
				  "success": false,
				  "code": "M004",
				  "message": "인증 코드가 올바르지 않거나 만료되었습니다."
				}""")))
	})
	ApiResult<Void> verifyResetCode(EmailVerifyRequest request);

	@Operation(summary = "비밀번호 재설정",
		description = "이메일 인증을 완료한 이메일의 비밀번호를 새 비밀번호로 재설정합니다.")
	@RequestBody(content = @Content(examples = @ExampleObject(value = """
		{
		  "email": "test@bookpool.kr",
		  "newPassword": "newPassword1234"
		}""")))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "재설정 성공"),
		@ApiResponse(
			responseCode = "400", description = "이메일 인증 미완료",
			content = @Content(examples = @ExampleObject(value = """
				{
				  "success": false,
				  "code": "M006",
				  "message": "비밀번호 재설정 이메일 인증이 완료되지 않았습니다."
				}""")))
	})
	ApiResult<Void> resetPassword(PasswordResetRequest request);
}
