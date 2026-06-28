package kr.co.bookpool.app.member.dto.response;

import kr.co.bookpool.app.member.entity.Member;
import kr.co.bookpool.app.member.entity.Role;

public record MeResponse(
	Long id,
	String email,
	String nickname,
	String contact,
	boolean emailSubscribed,
	Role role
) {

	public static MeResponse from(Member member) {
		return new MeResponse(
			member.getId(),
			member.getEmail(),
			member.getNickname(),
			member.getContact(),
			member.isEmailSubscribed(),
			member.getRole()
		);
	}
}
