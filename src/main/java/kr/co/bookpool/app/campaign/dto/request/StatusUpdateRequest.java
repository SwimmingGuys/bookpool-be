package kr.co.bookpool.app.campaign.dto.request;

import jakarta.validation.constraints.NotNull;
import kr.co.bookpool.app.campaign.entity.CampaignStatus;

public record StatusUpdateRequest(
	@NotNull CampaignStatus status
) {
}
