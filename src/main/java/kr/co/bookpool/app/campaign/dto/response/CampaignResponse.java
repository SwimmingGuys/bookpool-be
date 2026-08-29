package kr.co.bookpool.app.campaign.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import kr.co.bookpool.app.campaign.entity.Campaign;
import kr.co.bookpool.app.campaign.entity.CampaignStatus;
import kr.co.bookpool.app.campaign.entity.CampaignType;
import kr.co.bookpool.app.campaign.entity.ReviewChannel;

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
	String description,
	String applyUrl,

	// 모집 조건
	Integer capacity,
	String bookFormat,
	List<String> reviewChannels,
	LocalDate reviewDueDate,
	String requirements,

	// 수집 출처
	String source,
	String sourceUrl,
	LocalDateTime collectedAt,
	String publishStatus
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
			campaign.getCategory().getLabel(),
			campaign.getViewCount() == null ? 0 : campaign.getViewCount(),
			status,
			campaign.getRecruitStartDate(),
			campaign.getDeadlineAt().toLocalDate(),
			campaign.getAnnouncementDate(),
			campaign.getImageUrl(),
			campaign.getDescription(),
			campaign.getApplyUrl(),
			campaign.getCapacity(),
			campaign.getBookFormat() == null ? null : campaign.getBookFormat().name(),
			toChannelNames(campaign.getReviewChannels()),
			campaign.getReviewDueDate(),
			campaign.getRequirements(),
			campaign.getSource().name(),
			campaign.getSourceUrl(),
			campaign.getCollectedAt(),
			campaign.getPublishStatus().name()
		);
	}

	private static String toBadgeLabel(CampaignType type) {
		return type == CampaignType.REVIEWER ? "Reviewer" : "Beta Reader";
	}

	private static List<String> toChannelNames(List<ReviewChannel> channels) {
		if (channels == null) return List.of();
		return channels.stream()
			.map(channel -> channel.name().toUpperCase(Locale.ROOT))
			.toList();
	}
}
