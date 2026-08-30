package kr.co.bookpool.app.notification.entity;

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
import kr.co.bookpool.app.campaign.entity.Campaign;
import kr.co.bookpool.app.member.entity.Member;
import kr.co.bookpool.common.entity.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자별 알림 큐.
 *
 * <p>구독 설정은 저장되고 있었지만 알림이 실제로 쌓이는 곳이 없어 헤더의 벨이 항상 0이었다.
 */
@Getter
@Entity
@Table(
	name = "notification",
	indexes = {
		@Index(name = "idx_notification_member_created", columnList = "member_id, created_at"),
		@Index(name = "idx_notification_member_read", columnList = "member_id, is_read")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private NotificationKind kind;

	/** 공고와 무관한 알림(공지 등)도 있을 수 있어 nullable. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "campaign_id")
	private Campaign campaign;

	@Column(name = "message", length = 500)
	private String message;

	@Column(name = "is_read", nullable = false)
	private boolean read;

	private Notification(Member member, NotificationKind kind, Campaign campaign, String message) {
		this.member = member;
		this.kind = kind;
		this.campaign = campaign;
		this.message = message;
		this.read = false;
	}

	public static Notification of(Member member, NotificationKind kind, Campaign campaign, String message) {
		return new Notification(member, kind, campaign, message);
	}

	public void markRead() {
		this.read = true;
	}

	public boolean isOwnedBy(Long memberId) {
		return member.getId().equals(memberId);
	}
}
