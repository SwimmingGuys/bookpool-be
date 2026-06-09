package kr.co.bookpool.app.notice.entity;

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
	name = "notice",
	indexes = {
		@Index(name = "idx_notice_admin_id", columnList = "admin_id"),
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
}
