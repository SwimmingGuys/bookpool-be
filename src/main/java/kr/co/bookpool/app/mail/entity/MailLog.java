package kr.co.bookpool.app.mail.entity;

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
import kr.co.bookpool.app.campaign.entity.Campaign;
import kr.co.bookpool.app.member.entity.Member;
import kr.co.bookpool.common.entity.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "mail_log",
	indexes = {
		@Index(name = "idx_mail_log_member_id", columnList = "member_id"),
		@Index(name = "idx_mail_log_campaign_id", columnList = "campaign_id"),
		@Index(name = "idx_mail_log_status_created", columnList = "status, created_at")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MailLog extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "campaign_id", nullable = false)
	private Campaign campaign;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, columnDefinition = "ENUM('PENDING','SENT','FAILED') DEFAULT 'PENDING'")
	private MailLogStatus status = MailLogStatus.PENDING;

	@Column(name = "listing_count", nullable = false, columnDefinition = "INT DEFAULT 0")
	private Integer listingCount = 0;

	@Column(name = "fail_reason", length = 500)
	private String failReason;

	@Column(name = "retry_count", nullable = false, columnDefinition = "INT DEFAULT 0")
	private Integer retryCount = 0;

	@Column(name = "sent_at")
	private LocalDateTime sentAt;
}
