package kr.co.bookpool.app.campaign.dto.request;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.co.bookpool.app.campaign.entity.BookFormat;
import kr.co.bookpool.app.campaign.entity.CampaignCategory;
import kr.co.bookpool.app.campaign.entity.CampaignSource;
import kr.co.bookpool.app.campaign.entity.CampaignStatus;
import kr.co.bookpool.app.campaign.entity.CampaignType;
import kr.co.bookpool.app.campaign.entity.PublishStatus;
import kr.co.bookpool.app.campaign.entity.ReviewChannel;

public record CampaignUpdateRequest(
	@NotBlank String title,
	@NotBlank String bookTitle,
	@NotBlank String publisherName,
	@NotNull CampaignCategory category,
	@NotNull CampaignType type,
	String applyUrl,
	String imageUrl,
	String description,
	LocalDate recruitStartDate,
	@NotNull LocalDateTime deadlineAt,
	LocalDate announcementDate,
	@NotNull CampaignStatus status,

	// 모집 조건
	@Min(value = 1, message = "모집 인원은 1명 이상이어야 합니다.") Integer capacity,
	BookFormat bookFormat,
	List<ReviewChannel> reviewChannels,
	LocalDate reviewDueDate,
	String requirements,

	// 수집 출처
	CampaignSource source,
	String sourceUrl,
	PublishStatus publishStatus
) {
}
