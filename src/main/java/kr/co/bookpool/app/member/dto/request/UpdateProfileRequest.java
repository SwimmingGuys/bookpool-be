package kr.co.bookpool.app.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
	@NotBlank @Size(max = 50) String nickname,
	@Size(max = 100) String contact
) {
}
