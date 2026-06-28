package kr.co.bookpool.app.campaign.dto.request;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.co.bookpool.app.campaign.entity.CampaignCategory;
import kr.co.bookpool.app.campaign.entity.CampaignStatus;
import kr.co.bookpool.app.campaign.entity.CampaignType;

public record CampaignUpdateRequest(
	@NotBlank String title,
	@NotBlank String bookTitle,
	@NotBlank String publisherName,
	@NotNull CampaignCategory category,
	@NotNull CampaignType type,
	@NotBlank String applyUrl,
	String imageUrl,
	String description,
	LocalDate recruitStartDate,
	@NotNull LocalDateTime deadlineAt,
	LocalDate announcementDate,
	@NotNull CampaignStatus status
) {
}
