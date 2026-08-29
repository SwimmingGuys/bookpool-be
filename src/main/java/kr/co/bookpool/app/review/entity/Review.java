package kr.co.bookpool.app.review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kr.co.bookpool.app.campaign.entity.Campaign;
import kr.co.bookpool.app.campaign.entity.ReviewChannel;
import kr.co.bookpool.app.member.entity.Member;
import kr.co.bookpool.common.entity.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 참여자가 제출한 서평.
 *
 * <p>서평은 이 서비스의 최종 산출물인데, 그동안 관리자가 대신 입력하는 구조라
 * 참여자가 남길 방법이 없었다. 원문 링크를 받아 관리자가 인증하는 흐름으로 만든다.
 */
@Getter
@Entity
@Table(
	name = "review",
	uniqueConstraints = {
		// 한 사람이 같은 공고에 서평을 여러 번 남기지 못하게 한다.
		@UniqueConstraint(name = "uk_review_campaign_member", columnNames = {"campaign_id", "member_id"})
	},
	indexes = {
		@Index(name = "idx_review_campaign", columnList = "campaign_id"),
		@Index(name = "idx_review_member", columnList = "member_id"),
		@Index(name = "idx_review_submission_status", columnList = "submission_status")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "campaign_id", nullable = false)
	private Campaign campaign;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(nullable = false)
	private int rating;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(name = "channel", length = 20)
	private ReviewChannel channel;

	/** 서평 원문 주소. 관리자가 실제로 열어 확인한다. */
	@Column(name = "url", length = 500)
	private String url;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReviewStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "submission_status", nullable = false, length = 20)
	private ReviewSubmissionStatus submissionStatus;

	@Column(name = "reject_reason", length = 500)
	private String rejectReason;

	private Review(Campaign campaign, Member member, int rating, String content,
		ReviewChannel channel, String url) {
		this.campaign = campaign;
		this.member = member;
		this.rating = rating;
		this.content = content;
		this.channel = channel;
		this.url = url;
		this.status = ReviewStatus.VISIBLE;
		this.submissionStatus = ReviewSubmissionStatus.SUBMITTED;
	}

	public static Review create(Campaign campaign, Member member, int rating, String content,
		ReviewChannel channel, String url) {
		return new Review(campaign, member, rating, content, channel, url);
	}

	/** 본인이 내용을 고치면 다시 확인 대기로 돌아간다. */
	public void update(int rating, String content, ReviewChannel channel, String url) {
		this.rating = rating;
		this.content = content;
		this.channel = channel;
		this.url = url;
		this.submissionStatus = ReviewSubmissionStatus.SUBMITTED;
		this.rejectReason = null;
	}

	public void approve() {
		this.submissionStatus = ReviewSubmissionStatus.APPROVED;
		this.rejectReason = null;
	}

	public void reject(String reason) {
		this.submissionStatus = ReviewSubmissionStatus.REJECTED;
		this.rejectReason = reason;
	}

	public void changeStatus(ReviewStatus status) {
		this.status = status;
	}

	public boolean isOwnedBy(Long memberId) {
		return member.getId().equals(memberId);
	}
}
