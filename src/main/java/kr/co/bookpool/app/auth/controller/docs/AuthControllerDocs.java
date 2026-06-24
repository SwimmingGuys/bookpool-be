package kr.co.bookpool.app.auth.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.bookpool.app.auth.dto.request.LoginRequest;
import kr.co.bookpool.app.auth.dto.response.LoginResponse;
import kr.co.bookpool.common.response.ApiResult;

@Tag(name = "Auth", description = "인증 API")
public interface AuthControllerDocs {

	@Operation(summary = "로그인",
		description = "이메일/비밀번호로 로그인합니다. Access 토큰은 응답 바디로, Refresh 토큰은 httpOnly 쿠키로 발급됩니다.")
	@RequestBody(content = @Content(examples = @ExampleObject(value = """
		{
		  "email": "test@bookpool.kr",
		  "password": "password1234"
		}""")))
	@ApiResponses({
		@ApiResponse(
			responseCode = "200", description = "로그인 성공 (Set-Cookie: refreshToken)",
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
	ApiResult<LoginResponse> login(LoginRequest request, HttpServletResponse response);

	@Operation(summary = "토큰 재발급",
		description = "httpOnly 쿠키의 Refresh 토큰을 검증하고 새 Access 토큰을 발급합니다. "
			+ "Refresh 토큰도 함께 회전(재발급)되어 쿠키로 갱신됩니다.")
	@ApiResponses({
		@ApiResponse(
			responseCode = "200", description = "재발급 성공 (Set-Cookie: refreshToken)",
			content = @Content(examples = @ExampleObject(value = """
				{
				  "success": true,
				  "code": "SUCCESS",
				  "message": "요청에 성공했습니다.",
				  "data": { "accessToken": "eyJhbGciOiJIUzI1NiJ9...", "tokenType": "Bearer" }
				}"""))),
		@ApiResponse(
			responseCode = "401", description = "Refresh 토큰이 없거나 유효하지 않음",
			content = @Content(examples = @ExampleObject(value = """
				{
				  "success": false,
				  "code": "A003",
				  "message": "유효하지 않은 리프레시 토큰입니다. 다시 로그인해 주세요."
				}""")))
	})
	ApiResult<LoginResponse> reissue(String refreshToken, HttpServletResponse response);

	@Operation(summary = "로그아웃",
		description = "서버에 저장된 Refresh 토큰을 제거하고 쿠키를 만료시킵니다.")
	@ApiResponse(responseCode = "200", description = "로그아웃 성공 (Set-Cookie: refreshToken 만료)")
	ApiResult<Void> logout(String refreshToken, HttpServletResponse response);
}
