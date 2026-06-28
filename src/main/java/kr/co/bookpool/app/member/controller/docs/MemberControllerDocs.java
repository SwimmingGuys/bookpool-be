package kr.co.bookpool.app.member.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.bookpool.app.member.dto.request.SignUpRequest;
import kr.co.bookpool.app.member.dto.request.UpdateProfileRequest;
import kr.co.bookpool.app.member.dto.response.MeResponse;
import kr.co.bookpool.app.member.dto.response.SignUpResponse;
import kr.co.bookpool.common.response.ApiResult;

@Tag(name = "Member", description = "회원 API")
public interface MemberControllerDocs {

	@Operation(summary = "회원가입",
		description = "이메일/닉네임/비밀번호로 신규 회원을 등록합니다. "
			+ "사전에 이메일 인증(코드 발송 → 코드 확인)을 완료한 이메일만 가입할 수 있습니다.")
	@RequestBody(content = @Content(examples = @ExampleObject(value = """
		{
		  "email": "test@bookpool.kr",
		  "nickname": "북풀러",
		  "password": "password1234",
		  "emailSubscribed": true
		}""")))
	@ApiResponses({
		@ApiResponse(
			responseCode = "201", description = "회원가입 성공",
			content = @Content(examples = @ExampleObject(value = """
				{
				  "success": true,
				  "code": "SUCCESS",
				  "message": "요청에 성공했습니다.",
				  "data": { "id": 1, "email": "test@bookpool.kr", "nickname": "북풀러" }
				}"""))),
		@ApiResponse(
			responseCode = "400", description = "입력값 검증 실패",
			content = @Content(examples = @ExampleObject(value = """
				{
				  "success": false,
				  "code": "C001",
				  "message": "잘못된 입력값입니다.",
				  "data": [
				    { "field": "email", "message": "올바른 이메일 형식이 아닙니다." },
				    { "field": "password", "message": "비밀번호는 8자 이상 64자 이하여야 합니다." }
				  ]
				}"""))),
		@ApiResponse(
			responseCode = "409", description = "이미 사용 중인 이메일",
			content = @Content(examples = @ExampleObject(value = """
				{
				  "success": false,
				  "code": "M001",
				  "message": "이미 사용 중인 이메일입니다."
				}""")))
	})
	ApiResult<SignUpResponse> signUp(SignUpRequest request);

	@Operation(
		summary = "내 정보 조회",
		description = "Access 토큰으로 인증된 회원의 정보를 조회합니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
		@ApiResponse(
			responseCode = "200", description = "조회 성공",
			content = @Content(examples = @ExampleObject(value = """
				{
				  "success": true,
				  "code": "SUCCESS",
				  "message": "요청에 성공했습니다.",
				  "data": { "id": 1, "email": "test@bookpool.kr", "nickname": "북풀러", "contact": "010-1234-5678", "emailSubscribed": true, "role": "USER" }
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
	ApiResult<MeResponse> getMyInfo(Long memberId);

	@Operation(
		summary = "프로필 수정",
		description = "닉네임/연락처를 수정합니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "수정 성공"),
		@ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
		@ApiResponse(responseCode = "401", description = "인증 실패")
	})
	ApiResult<MeResponse> updateProfile(Long memberId, UpdateProfileRequest request);
}