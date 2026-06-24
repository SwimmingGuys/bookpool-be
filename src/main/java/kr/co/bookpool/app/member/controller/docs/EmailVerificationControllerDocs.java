package kr.co.bookpool.app.member.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.bookpool.app.member.dto.request.EmailCodeRequest;
import kr.co.bookpool.app.member.dto.request.EmailVerifyRequest;
import kr.co.bookpool.common.response.ApiResult;

@Tag(name = "EmailVerification", description = "회원가입 이메일 인증 API")
public interface EmailVerificationControllerDocs {

	@Operation(summary = "인증 코드 발송",
		description = "회원가입할 이메일로 6자리 인증 코드를 발송합니다. 이미 가입된 이메일이면 409를 반환합니다.")
	@RequestBody(content = @Content(examples = @ExampleObject(value = """
		{
		  "email": "test@bookpool.kr"
		}""")))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "발송 성공"),
		@ApiResponse(
			responseCode = "409", description = "이미 사용 중인 이메일",
			content = @Content(examples = @ExampleObject(value = """
				{
				  "success": false,
				  "code": "M001",
				  "message": "이미 사용 중인 이메일입니다."
				}""")))
	})
	ApiResult<Void> sendCode(EmailCodeRequest request);

	@Operation(summary = "인증 코드 확인",
		description = "발송된 코드를 검증합니다. 성공하면 해당 이메일은 일정 시간 동안 '인증 완료' 상태가 되어 회원가입이 가능합니다.")
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
	ApiResult<Void> verify(EmailVerifyRequest request);
}
