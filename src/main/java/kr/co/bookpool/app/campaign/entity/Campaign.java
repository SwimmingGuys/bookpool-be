package kr.co.bookpool.app.campaign.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import kr.co.bookpool.common.entity.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "campaign",
	indexes = {
		@Index(name = "idx_campaign_status_deadline", columnList = "status, deadline_at"),
		@Index(name = "idx_campaign_category", columnList = "category"),
		@Index(name = "idx_campaign_created_at", columnList = "created_at"),
		@Index(name = "idx_campaign_publish_status", columnList = "publish_status"),
		@Index(name = "idx_campaign_publisher_name", columnList = "publisher_name")
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
	@Column(nullable = false, length = 30)
	private CampaignCategory category;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, columnDefinition = "ENUM('REVIEWER','BETA_READER')")
	private CampaignType type;

	/**
	 * 신청 페이지(구글폼·출판사 페이지 등).
	 * 수집된 공고는 신청 링크를 못 찾는 경우가 있어 nullable로 둔다.
	 * 링크가 없으면 프론트가 신청 버튼을 비활성화한다.
	 */
	@Column(name = "apply_url", length = 500)
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

	// ---------- 모집 조건 ----------

	/** 모집 인원. 공고에 적혀 있지 않으면 null. */
	@Column(name = "capacity")
	private Integer capacity;

	@Enumerated(EnumType.STRING)
	@Column(name = "book_format", length = 20)
	private BookFormat bookFormat;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(
		name = "campaign_review_channel",
		joinColumns = @JoinColumn(name = "campaign_id"),
		indexes = @Index(name = "idx_campaign_review_channel", columnList = "campaign_id")
	)
	@Enumerated(EnumType.STRING)
	@Column(name = "channel", nullable = false, length = 20)
	private List<ReviewChannel> reviewChannels = new ArrayList<>();

	/** 서평 제출 기한. */
	@Column(name = "review_due_date")
	private LocalDate reviewDueDate;

	/** 신청 자격 (SNS 팔로워 수, 이전 활동 이력 등). */
	@Column(name = "requirements", columnDefinition = "TEXT")
	private String requirements;

	// ---------- 수집 출처 ----------

	@Enumerated(EnumType.STRING)
	@Column(name = "source", nullable = false, length = 20)
	private CampaignSource source = CampaignSource.MANUAL;

	@Column(name = "source_url", length = 500)
	private String sourceUrl;

	@Column(name = "collected_at")
	private LocalDateTime collectedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "publish_status", nullable = false, length = 20)
	private PublishStatus publishStatus = PublishStatus.PUBLISHED;

	/**
	 * 같은 공고가 여러 소스에서 수집될 때 중복을 걸러내기 위한 키.
	 * 도서명 + 출판사 + 마감일이 같으면 같은 모집으로 본다.
	 */
	@Column(name = "dedupe_key", length = 500)
	private String dedupeKey;

	@Builder
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
		CampaignStatus status,
		Integer capacity,
		BookFormat bookFormat,
		List<ReviewChannel> reviewChannels,
		LocalDate reviewDueDate,
		String requirements,
		CampaignSource source,
		String sourceUrl,
		LocalDateTime collectedAt,
		PublishStatus publishStatus
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
		this.status = status == null ? CampaignStatus.OPEN : status;
		this.capacity = capacity;
		this.bookFormat = bookFormat;
		this.reviewChannels = dedupeChannels(reviewChannels);
		this.reviewDueDate = reviewDueDate;
		this.requirements = requirements;
		this.source = source == null ? CampaignSource.MANUAL : source;
		this.sourceUrl = sourceUrl;
		this.collectedAt = collectedAt;
		this.publishStatus = publishStatus == null ? PublishStatus.PUBLISHED : publishStatus;
		this.dedupeKey = dedupeKey(bookTitle, publisherName, deadlineAt);
	}

	/**
	 * 모집 조건·수집 출처가 없던 시절의 생성 시그니처.
	 * 새 코드는 빌더를 쓰고, 이 메서드는 기존 호출부(테스트 등) 호환용으로 남긴다.
	 */
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
		return Campaign.builder()
			.title(title)
			.bookTitle(bookTitle)
			.publisherName(publisherName)
			.category(category)
			.type(type)
			.applyUrl(applyUrl)
			.imageUrl(imageUrl)
			.description(description)
			.recruitStartDate(recruitStartDate)
			.deadlineAt(deadlineAt)
			.announcementDate(announcementDate)
			.status(status)
			.build();
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
		CampaignStatus status,
		Integer capacity,
		BookFormat bookFormat,
		List<ReviewChannel> reviewChannels,
		LocalDate reviewDueDate,
		String requirements,
		CampaignSource source,
		String sourceUrl,
		PublishStatus publishStatus
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
		if (status != null) this.status = status;
		this.capacity = capacity;
		this.bookFormat = bookFormat;
		// 컬렉션은 새 리스트로 갈아끼우지 않고 내용만 교체한다(orphan 삭제가 정상 동작하도록).
		this.reviewChannels.clear();
		this.reviewChannels.addAll(dedupeChannels(reviewChannels));
		this.reviewDueDate = reviewDueDate;
		this.requirements = requirements;
		if (source != null) this.source = source;
		this.sourceUrl = sourceUrl;
		if (publishStatus != null) this.publishStatus = publishStatus;
		this.dedupeKey = dedupeKey(bookTitle, publisherName, deadlineAt);
	}

	public void changeStatus(CampaignStatus status) {
		this.status = status;
	}

	public void changePublishStatus(PublishStatus publishStatus) {
		this.publishStatus = publishStatus;
	}

	public boolean isPublished() {
		return publishStatus == PublishStatus.PUBLISHED;
	}

	/** 도서명 + 출판사 + 마감일(일 단위). 공백과 대소문자는 무시한다. */
	public static String dedupeKey(String bookTitle, String publisherName, LocalDateTime deadlineAt) {
		if (bookTitle == null || publisherName == null || deadlineAt == null) return null;
		return normalize(bookTitle) + "|" + normalize(publisherName) + "|" + deadlineAt.toLocalDate();
	}

	private static String normalize(String value) {
		return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
	}

	private static List<ReviewChannel> dedupeChannels(List<ReviewChannel> channels) {
		if (channels == null || channels.isEmpty()) return new ArrayList<>();
		return new ArrayList<>(new LinkedHashSet<>(channels));
	}
}
