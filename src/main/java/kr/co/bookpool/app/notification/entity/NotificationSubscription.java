package kr.co.bookpool.app.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kr.co.bookpool.app.member.entity.Member;
import kr.co.bookpool.common.entity.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원당 1건의 알림 구독 설정.
 * 다중 값(타입/카테고리/출판사)은 JSON 문자열로 저장한다.
 */
@Getter
@Entity
@Table(
	name = "notification_subscription",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_notification_subscription_member", columnNames = "member_id")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSubscription extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(name = "types_json", nullable = false, columnDefinition = "TEXT")
	private String typesJson;

	@Column(name = "categories_json", nullable = false, columnDefinition = "TEXT")
	private String categoriesJson;

	@Column(name = "publishers_json", nullable = false, columnDefinition = "TEXT")
	private String publishersJson;

	private NotificationSubscription(Member member, String typesJson, String categoriesJson, String publishersJson) {
		this.member = member;
		this.typesJson = typesJson;
		this.categoriesJson = categoriesJson;
		this.publishersJson = publishersJson;
	}

	public static NotificationSubscription create(Member member, String typesJson,
		String categoriesJson, String publishersJson) {
		return new NotificationSubscription(member, typesJson, categoriesJson, publishersJson);
	}

	public void update(String typesJson, String categoriesJson, String publishersJson) {
		this.typesJson = typesJson;
		this.categoriesJson = categoriesJson;
		this.publishersJson = publishersJson;
	}
}
