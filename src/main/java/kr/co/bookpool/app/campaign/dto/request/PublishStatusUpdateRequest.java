package kr.co.bookpool.app.campaign.dto.request;

import jakarta.validation.constraints.NotNull;
import kr.co.bookpool.app.campaign.entity.PublishStatus;

public record PublishStatusUpdateRequest(
	@NotNull PublishStatus publishStatus
) {
}
