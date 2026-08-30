package kr.co.bookpool.app.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kr.co.bookpool.app.campaign.entity.ReviewChannel;

public record ReviewRequest(
	// 수정 시에는 경로의 id를 쓰므로 생성에서만 필요하다.
	Long campaignId,
	@Min(1) @Max(5) int rating,
	@NotBlank @Size(max = 2000) String content,
	/**
	 * 외부에 올린 서평의 채널과 원문 링크.
	 *
	 * 이 기능은 책 서평을 옮겨 적는 곳이 아니라 모집에 참여한 경험을 남기는 곳이다.
	 * 서평을 아직 안 썼거나 링크를 공개하고 싶지 않은 참여자도 후기를 남길 수 있어야 해서
	 * 둘 다 선택으로 둔다. 엔티티의 두 컬럼도 원래 nullable이라 스키마 변경은 없다.
	 */
	ReviewChannel channel,
	@Size(max = 500) String url
) {
}
