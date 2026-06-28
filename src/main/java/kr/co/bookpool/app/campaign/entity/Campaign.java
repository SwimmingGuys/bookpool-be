package kr.co.bookpool.app.campaign.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import kr.co.bookpool.common.entity.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "campaign",
	indexes = {
		@Index(name = "idx_campaign_status_deadline", columnList = "status, deadline_at"),
		@Index(name = "idx_campaign_category", columnList = "category"),
		@Index(name = "idx_campaign_created_at", columnList = "created_at")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Campaign extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 255)
	private String title;

	@Column(name = "book_title", nullable = false, length = 255)
	private String bookTitle;

	@Column(name = "publisher_name", nullable = false, length = 100)
	private String publisherName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, columnDefinition = "ENUM('IT','ECONOMY','NOVEL','ESSAY','HUMANITY','SCIENCE','ETC')")
	private CampaignCategory category;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, columnDefinition = "ENUM('REVIEWER','BETA_READER')")
	private CampaignType type;

	@Column(name = "apply_url", nullable = false, length = 500)
	private String applyUrl;

	@Column(name = "image_url", length = 500)
	private String imageUrl;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "recruit_start_date")
	private LocalDate recruitStartDate;

	@Column(name = "deadline_at", nullable = false)
	private LocalDateTime deadlineAt;

	@Column(name = "announcement_date")
	private LocalDate announcementDate;

	@Column(name = "view_count", nullable = false)
	private Integer viewCount = 0;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, columnDefinition = "ENUM('UPCOMING','OPEN','CLOSED') DEFAULT 'OPEN'")
	private CampaignStatus status = CampaignStatus.OPEN;

	private Campaign(
		String title,
		String bookTitle,
		String publisherName,
		CampaignCategory category,
		CampaignType type,
		String applyUrl,
		String imageUrl,
		String description,
		LocalDate recruitStartDate,
		LocalDateTime deadlineAt,
		LocalDate announcementDate,
		CampaignStatus status
	) {
		this.title = title;
		this.bookTitle = bookTitle;
		this.publisherName = publisherName;
		this.category = category;
		this.type = type;
		this.applyUrl = applyUrl;
		this.imageUrl = imageUrl;
		this.description = description;
		this.recruitStartDate = recruitStartDate;
		this.deadlineAt = deadlineAt;
		this.announcementDate = announcementDate;
		this.viewCount = 0;
		this.status = status;
	}

	public static Campaign create(
		String title,
		String bookTitle,
		String publisherName,
		CampaignCategory category,
		CampaignType type,
		String applyUrl,
		String imageUrl,
		String description,
		LocalDate recruitStartDate,
		LocalDateTime deadlineAt,
		LocalDate announcementDate,
		CampaignStatus status
	) {
		return new Campaign(title, bookTitle, publisherName, category, type, applyUrl, imageUrl,
			description, recruitStartDate, deadlineAt, announcementDate, status);
	}

	/**
	 * 관리자 수정용. 식별자/조회수/생성시각은 보존하고 편집 가능한 필드만 갱신한다.
	 */
	public void update(
		String title,
		String bookTitle,
		String publisherName,
		CampaignCategory category,
		CampaignType type,
		String applyUrl,
		String imageUrl,
		String description,
		LocalDate recruitStartDate,
		LocalDateTime deadlineAt,
		LocalDate announcementDate,
		CampaignStatus status
	) {
		this.title = title;
		this.bookTitle = bookTitle;
		this.publisherName = publisherName;
		this.category = category;
		this.type = type;
		this.applyUrl = applyUrl;
		this.imageUrl = imageUrl;
		this.description = description;
		this.recruitStartDate = recruitStartDate;
		this.deadlineAt = deadlineAt;
		this.announcementDate = announcementDate;
		this.status = status;
	}
}
