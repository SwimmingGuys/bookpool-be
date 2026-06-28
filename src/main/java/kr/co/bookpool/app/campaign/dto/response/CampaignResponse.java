package kr.co.bookpool.app.campaign.dto.response;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import kr.co.bookpool.app.campaign.entity.Campaign;
import kr.co.bookpool.app.campaign.entity.CampaignStatus;
import kr.co.bookpool.app.campaign.entity.CampaignType;

public record CampaignResponse(
	String id,
	String badgeLabel,
	long daysRemaining,
	String title,
	String bookTitle,
	String publisher,
	String category,
	int viewCount,
	String status,
	LocalDate recruitStartDate,
	LocalDate recruitEndDate,
	LocalDate announcementDate,
	String coverImage,
	String description
) {

	public static CampaignResponse from(Campaign campaign) {
		long days = ChronoUnit.DAYS.between(LocalDate.now(), campaign.getDeadlineAt().toLocalDate());
		String status = campaign.getStatus() == CampaignStatus.CLOSED || days < 0 ? "closed" : "open";

		return new CampaignResponse(
			String.valueOf(campaign.getId()),
			toBadgeLabel(campaign.getType()),
			days,
			campaign.getTitle(),
			campaign.getBookTitle(),
			campaign.getPublisherName(),
			toCategoryLabel(campaign.getCategory().name()),
			campaign.getViewCount() == null ? 0 : campaign.getViewCount(),
			status,
			campaign.getRecruitStartDate(),
			campaign.getDeadlineAt().toLocalDate(),
			campaign.getAnnouncementDate(),
			campaign.getImageUrl(),
			campaign.getDescription()
		);
	}

	private static String toBadgeLabel(CampaignType type) {
		return type == CampaignType.REVIEWER ? "Reviewer" : "Beta Reader";
	}

	private static String toCategoryLabel(String enumName) {
		return switch (enumName) {
			case "IT" -> "IT/개발";
			case "ECONOMY" -> "경제";
			case "NOVEL" -> "소설";
			case "ESSAY" -> "에세이";
			case "HUMANITY" -> "인문/사회";
			case "SCIENCE" -> "예술/디자인";
			default -> "기타";
		};
	}
}
