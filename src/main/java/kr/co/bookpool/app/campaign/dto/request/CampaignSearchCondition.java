package kr.co.bookpool.app.campaign.dto.request;

import java.util.List;

import kr.co.bookpool.app.campaign.entity.CampaignCategory;
import kr.co.bookpool.app.campaign.entity.CampaignType;
import lombok.Builder;

public record CampaignSearchCondition(
	String query,
	List<CampaignCategory> categories,
	List<CampaignType> types,
	DeadlineFilter deadline,
	SortKey sort
) {

	@Builder
	public CampaignSearchCondition {
		if (deadline == null) deadline = DeadlineFilter.ALL;
		if (sort == null) sort = SortKey.DEADLINE;
	}
}
