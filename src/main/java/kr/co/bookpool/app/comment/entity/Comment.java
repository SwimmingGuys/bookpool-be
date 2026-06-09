package kr.co.bookpool.app.comment.entity;

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
import kr.co.bookpool.app.article.entity.Article;
import kr.co.bookpool.app.member.entity.Member;
import kr.co.bookpool.app.notice.entity.Notice;
import kr.co.bookpool.common.entity.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "`comment`",
	indexes = {
		@Index(name = "idx_comment_article", columnList = "article_id, created_at"),
		@Index(name = "idx_comment_notice", columnList = "notice_id, created_at"),
		@Index(name = "idx_comment_parent", columnList = "parent_comment_id"),
		@Index(name = "idx_comment_member", columnList = "member_id")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "article_id")
	private Article article;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "notice_id")
	private Notice notice;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_comment_id")
	private Comment parentComment;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(nullable = false, columnDefinition = "TINYINT DEFAULT 0")
	private Integer depth = 0;

	@Column(name = "is_deleted", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
	private Boolean deleted = false;
}
