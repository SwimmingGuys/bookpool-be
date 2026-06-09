package kr.co.bookpool.app.article.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
	name = "article",
	indexes = {
		@Index(name = "idx_article_member_id", columnList = "member_id"),
		@Index(name = "idx_article_created_at", columnList = "created_at"),
		@Index(name = "idx_article_deleted_created", columnList = "is_deleted, created_at")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Article extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(nullable = false, length = 255)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(name = "view_count", nullable = false, columnDefinition = "INT DEFAULT 0")
	private Integer viewCount = 0;

	@Column(name = "is_deleted", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
	private Boolean deleted = false;
}
