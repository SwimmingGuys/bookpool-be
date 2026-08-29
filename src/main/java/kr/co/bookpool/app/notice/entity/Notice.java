package kr.co.bookpool.app.notice.entity;

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
import kr.co.bookpool.app.member.entity.Member;
import kr.co.bookpool.common.entity.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "notice",
	indexes = {
		@Index(name = "idx_notice_admin_id", columnList = "admin_id"),
		@Index(name = "idx_notice_category", columnList = "category"),
		@Index(name = "idx_notice_created_at", columnList = "created_at")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "admin_id", nullable = false)
	private Member admin;

	@Column(nullable = false, length = 255)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, columnDefinition = "ENUM('UPDATE','POLICY','MAINTENANCE','EVENT','GENERAL') DEFAULT 'GENERAL'")
	private NoticeCategory category;

	@Column(nullable = false)
	private boolean pinned;

	private Notice(Member admin, String title, String content, NoticeCategory category, boolean pinned) {
		this.admin = admin;
		this.title = title;
		this.content = content;
		this.category = category;
		this.pinned = pinned;
	}

	public static Notice create(Member admin, String title, String content, NoticeCategory category, boolean pinned) {
		return new Notice(admin, title, content, category, pinned);
	}

	/** 작성자와 생성시각은 보존하고 본문만 갱신한다. */
	public void update(String title, String content, NoticeCategory category, boolean pinned) {
		this.title = title;
		this.content = content;
		this.category = category;
		this.pinned = pinned;
	}
}
