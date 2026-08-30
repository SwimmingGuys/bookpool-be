package kr.co.bookpool.app.campaign.dto.request;

import java.time.LocalDate;
import java.util.List;

import kr.co.bookpool.app.campaign.entity.CampaignCategory;
import kr.co.bookpool.app.campaign.entity.CampaignType;
import kr.co.bookpool.app.campaign.entity.PublishStatus;
import lombok.Builder;

public record CampaignSearchCondition(
	String query,
	String publisher,
	List<CampaignCategory> categories,
	List<CampaignType> types,
	DeadlineFilter deadline,
	/** 마감까지 남은 일수 상한. deadline enum보다 우선한다. */
	Integer withinDays,
	LocalDate from,
	LocalDate to,
	DateBasis dateBasis,
	/** null이면 게시 상태로 거르지 않는다(관리자 조회). */
	PublishStatus publishStatus,
	SortKey sort
) {

	@Builder
	public CampaignSearchCondition {
		if (deadline == null) deadline = DeadlineFilter.ALL;
		if (sort == null) sort = SortKey.DEADLINE;
		if (dateBasis == null) dateBasis = DateBasis.RECRUIT_END;
	}

	/**
	 * 마감 조건을 남은 일수로 통일한다.
	 * 프론트는 withinDays를 보내고, 이전 클라이언트는 deadline enum을 보낸다.
	 */
	public Integer effectiveWithinDays() {
		if (withinDays != null) return Math.max(withinDays, 0);
		return switch (deadline) {
			case WEEK -> 7;
			case IMMINENT -> 3;
			case ALL -> null;
		};
	}
}
