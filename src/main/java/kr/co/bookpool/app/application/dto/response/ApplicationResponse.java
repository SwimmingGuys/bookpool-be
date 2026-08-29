package kr.co.bookpool.app.application.dto.response;

import java.time.LocalDateTime;

import kr.co.bookpool.app.application.entity.Application;
import kr.co.bookpool.app.campaign.dto.response.CampaignResponse;

/**
 * 마이페이지의 '신청한 공고'.
 * 발표일·서평 제출 기한을 함께 보여줘야 해서 공고 정보를 통째로 담는다.
 */
public record ApplicationResponse(
	String id,
	String status,
	LocalDateTime appliedAt,
	CampaignResponse campaign
) {

	public static ApplicationResponse from(Application application) {
		return new ApplicationResponse(
			String.valueOf(application.getId()),
			application.getStatus().name(),
			application.getCreatedAt(),
			CampaignResponse.from(application.getCampaign())
		);
	}
}
