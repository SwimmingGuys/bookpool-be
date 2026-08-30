package kr.co.bookpool.app.campaign.service;

import static kr.co.bookpool.common.exception.ErrorCode.*;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.co.bookpool.app.campaign.dto.request.CampaignCreateRequest;
import kr.co.bookpool.app.campaign.dto.request.CampaignSearchCondition;
import kr.co.bookpool.app.campaign.dto.request.CampaignUpdateRequest;
import kr.co.bookpool.app.campaign.dto.response.CampaignResponse;
import kr.co.bookpool.app.campaign.entity.Campaign;
import kr.co.bookpool.app.campaign.entity.CampaignStatus;
import kr.co.bookpool.app.campaign.entity.PublishStatus;
import kr.co.bookpool.app.campaign.repository.CampaignRepository;
import kr.co.bookpool.app.campaign.repository.CampaignSpecifications;
import kr.co.bookpool.app.notification.service.NotificationFanoutService;
import kr.co.bookpool.common.exception.BusinessException;
import kr.co.bookpool.common.response.PageResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCampaignService {

	private static final int MAX_PAGE_SIZE = 100;

	private final CampaignRepository campaignRepository;
	private final NotificationFanoutService notificationFanoutService;

	/**
	 * 백오피스 목록. publishStatus로 검수 큐(DRAFT)와 게시된 공고를 나눠 본다.
	 * publishStatus가 null이면 상태와 무관하게 전부 조회한다.
	 */
	public PageResponse<CampaignResponse> list(
		PublishStatus publishStatus,
		String query,
		int page,
		int size
	) {
		// 잘못된 페이지 파라미터로 PageRequest가 예외(→500)를 던지지 않도록 방어적으로 보정한다.
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		// 최신 등록순. createdAt 동률 시 id를 2차 키로 둬 순서를 안정화.
		Sort sort = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
		Pageable pageable = PageRequest.of(safePage, safeSize, sort);

		CampaignSearchCondition condition = CampaignSearchCondition.builder()
			.query(query)
			.publishStatus(publishStatus)
			.build();
		Specification<Campaign> spec = CampaignSpecifications.of(condition);

		return PageResponse.from(campaignRepository.findAll(spec, pageable).map(CampaignResponse::from));
	}

	public CampaignResponse get(Long campaignId) {
		return CampaignResponse.from(findById(campaignId));
	}

	@Transactional
	public CampaignResponse create(CampaignCreateRequest request) {
		Campaign saved = campaignRepository.save(request.toEntity());
		// 검수 대기로 저장한 공고는 게시될 때 알린다.
		notificationFanoutService.notifyNewCampaign(saved);
		return CampaignResponse.from(saved);
	}

	@Transactional
	public CampaignResponse update(Long campaignId, CampaignUpdateRequest request) {
		Campaign campaign = findById(campaignId);
		boolean wasPublished = campaign.isPublished();
		campaign.update(
			request.title(), request.bookTitle(), request.publisherName(), request.category(),
			request.type(), request.applyUrl(), request.imageUrl(), request.description(),
			request.recruitStartDate(), request.deadlineAt(), request.announcementDate(), request.status(),
			request.capacity(), request.bookFormat(), request.reviewChannels(), request.reviewDueDate(),
			request.requirements(), request.source(), request.sourceUrl(), request.publishStatus()
		);
		// 검수를 통과해 처음 공개되는 순간에만 알린다(수정 때마다 다시 보내지 않는다).
		if (!wasPublished && campaign.isPublished()) {
			notificationFanoutService.notifyNewCampaign(campaign);
		}
		return CampaignResponse.from(campaign);
	}

	/** 검수 큐 ↔ 게시 전환. */
	@Transactional
	public CampaignResponse changePublishStatus(Long campaignId, PublishStatus publishStatus) {
		Campaign campaign = findById(campaignId);
		boolean wasPublished = campaign.isPublished();
		campaign.changePublishStatus(publishStatus);
		if (!wasPublished && campaign.isPublished()) {
			notificationFanoutService.notifyNewCampaign(campaign);
		}
		return CampaignResponse.from(campaign);
	}

	/** 모집중 ↔ 마감 수동 전환. */
	@Transactional
	public CampaignResponse changeStatus(Long campaignId, CampaignStatus status) {
		Campaign campaign = findById(campaignId);
		campaign.changeStatus(status);
		return CampaignResponse.from(campaign);
	}

	@Transactional
	public void delete(Long campaignId) {
		campaignRepository.delete(findById(campaignId));
	}

	/**
	 * 같은 도서·출판사·마감일의 공고가 이미 있는지.
	 * 수집기가 같은 공고를 여러 소스에서 가져올 때 쓴다.
	 */
	public boolean existsDuplicate(String bookTitle, String publisherName, java.time.LocalDateTime deadlineAt) {
		String key = Campaign.dedupeKey(bookTitle, publisherName, deadlineAt);
		return key != null && campaignRepository.existsByDedupeKey(key);
	}

	private Campaign findById(Long campaignId) {
		return campaignRepository.findById(campaignId)
			.orElseThrow(() -> new BusinessException(CAMPAIGN_NOT_FOUND));
	}
}
