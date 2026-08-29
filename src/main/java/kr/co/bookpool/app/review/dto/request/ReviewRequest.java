package kr.co.bookpool.app.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.co.bookpool.app.campaign.entity.ReviewChannel;

public record ReviewRequest(
	// 수정 시에는 경로의 id를 쓰므로 생성에서만 필요하다.
	Long campaignId,
	@Min(1) @Max(5) int rating,
	@NotBlank @Size(max = 2000) String content,
	@NotNull ReviewChannel channel,
	@NotBlank @Size(max = 500) String url
) {
}
