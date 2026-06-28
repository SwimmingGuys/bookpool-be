package kr.co.bookpool.app.inquiry.entity;

import java.time.LocalDateTime;

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
	name = "inquiry",
	indexes = {
		@Index(name = "idx_inquiry_member_created", columnList = "member_id, created_at"),
		@Index(name = "idx_inquiry_status", columnList = "status")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private InquiryType type;

	@Column(nullable = false, length = 255)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private InquiryStatus status;

	@Column(columnDefinition = "TEXT")
	private String answer;

	@Column(name = "answered_at")
	private LocalDateTime answeredAt;

	private Inquiry(Member member, InquiryType type, String title, String content) {
		this.member = member;
		this.type = type;
		this.title = title;
		this.content = content;
		this.status = InquiryStatus.PENDING;
	}

	public static Inquiry create(Member member, InquiryType type, String title, String content) {
		return new Inquiry(member, type, title, content);
	}

	public void answer(String answer) {
		this.answer = answer;
		this.answeredAt = LocalDateTime.now();
		this.status = InquiryStatus.ANSWERED;
	}
}
