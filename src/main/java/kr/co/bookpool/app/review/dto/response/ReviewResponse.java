package kr.co.bookpool.app.review.dto.response;

import java.time.LocalDateTime;

import kr.co.bookpool.app.review.entity.Review;

public record ReviewResponse(
	String id,
	String campaignId,
	String campaignTitle,
	String bookTitle,
	String authorNickname,
	/** 조회한 사람이 쓴 서평인지. 로그인하지 않았으면 false. */
	boolean isMine,
	int rating,
	String content,
	String channel,
	String url,
	String status,
	String submissionStatus,
	String rejectReason,
	LocalDateTime createdAt
) {

	public static ReviewResponse of(Review review, Long viewerId) {
		return new ReviewResponse(
			String.valueOf(review.getId()),
			String.valueOf(review.getCampaign().getId()),
			review.getCampaign().getTitle(),
			review.getCampaign().getBookTitle(),
			review.getMember().getNickname(),
			viewerId != null && review.isOwnedBy(viewerId),
			review.getRating(),
			review.getContent(),
			review.getChannel() == null ? null : review.getChannel().name(),
			review.getUrl(),
			review.getStatus().name(),
			review.getSubmissionStatus().name(),
			review.getRejectReason(),
			review.getCreatedAt()
		);
	}
}
