package kr.co.bookpool.app.review.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.co.bookpool.app.review.entity.ReviewSubmissionStatus;

public record ReviewDecisionRequest(
	@NotNull ReviewSubmissionStatus submissionStatus,
	@Size(max = 500) String rejectReason
) {
}
