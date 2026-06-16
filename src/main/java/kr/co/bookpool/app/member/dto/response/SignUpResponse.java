package kr.co.bookpool.app.member.dto.response;

import kr.co.bookpool.app.member.entity.Member;

public record SignUpResponse(
	Long id,
	String email,
	String nickname
) {

	public static SignUpResponse from(Member member) {
		return new SignUpResponse(member.getId(), member.getEmail(), member.getNickname());
	}
}