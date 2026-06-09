package kr.co.bookpool.app.member.controller;

import static org.springframework.http.HttpStatus.*;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.co.bookpool.app.member.controller.docs.MemberControllerDocs;
import kr.co.bookpool.app.member.dto.request.SignUpRequest;
import kr.co.bookpool.app.member.dto.response.SignUpResponse;
import kr.co.bookpool.app.member.service.MemberService;
import kr.co.bookpool.common.response.ApiResult;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MemberController implements MemberControllerDocs {

	private final MemberService memberService;

	@Override
	@PostMapping("/api/signup")
	@ResponseStatus(CREATED)
	public ApiResult<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
		return ApiResult.success(memberService.signUp(request));
	}
}