package kr.co.bookpool.app.auth.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.bookpool.app.auth.dto.request.LoginRequest;
import kr.co.bookpool.app.auth.dto.response.LoginResponse;
import kr.co.bookpool.common.response.ApiResult;

@Tag(name = "Auth", description = "인증 API")
public interface AuthControllerDocs {

	@Operation(summary = "로그인", description = "이메일/비밀번호로 로그인하고 Access 토큰을 발급합니다.")
	@RequestBody(content = @Content(examples = @ExampleObject(value = """
		{
		  "email": "test@bookpool.kr",
		  "password": "password1234"
		}""")))
	@ApiResponses({
		@ApiResponse(
			responseCode = "200", description = "로그인 성공",
			content = @Content(examples = @ExampleObject(value = """
				{
				  "success": true,
				  "code": "SUCCESS",
				  "message": "요청에 성공했습니다.",
				  "data": { "accessToken": "eyJhbGciOiJIUzI1NiJ9...", "tokenType": "Bearer" }
				}"""))),
		@ApiResponse(
			responseCode = "400", description = "입력값 검증 실패",
			content = @Content(examples = @ExampleObject(value = """
				{
				  "success": false,
				  "code": "C001",
				  "message": "잘못된 입력값입니다.",
				  "data": [
				    { "field": "email", "message": "올바른 이메일 형식이 아닙니다." }
				  ]
				}"""))),
		@ApiResponse(
			responseCode = "401", description = "이메일 또는 비밀번호 불일치",
			content = @Content(examples = @ExampleObject(value = """
				{
				  "success": false,
				  "code": "A001",
				  "message": "이메일 또는 비밀번호가 올바르지 않습니다."
				}""")))
	})
	ApiResult<LoginResponse> login(LoginRequest request);
}
