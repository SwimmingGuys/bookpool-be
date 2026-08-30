package kr.co.bookpool.app.notification.entity;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

/** 알림 종류. 프론트가 라벨과 색을 여기에 맞춰 보여준다. */
public enum NotificationKind {
	/** 구독 조건에 맞는 새 공고가 게시됨 */
	NEW_RECRUITMENT,
	/** 즐겨찾기한 공고의 마감이 임박함 */
	DEADLINE_SOON,
	/** 발표일 안내 */
	ANNOUNCEMENT,
	ETC;

	@JsonCreator
	public static NotificationKind from(String value) {
		if (value == null || value.isBlank()) return ETC;
		String normalized = value.trim().toUpperCase(Locale.ROOT);
		for (NotificationKind kind : values()) {
			if (kind.name().equals(normalized)) return kind;
		}
		return ETC;
	}
}
