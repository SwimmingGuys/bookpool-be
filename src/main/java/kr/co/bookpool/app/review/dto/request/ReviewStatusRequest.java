package kr.co.bookpool.app.review.dto.request;

import jakarta.validation.constraints.NotNull;
import kr.co.bookpool.app.review.entity.ReviewStatus;

public record ReviewStatusRequest(
	@NotNull ReviewStatus status
) {
}
