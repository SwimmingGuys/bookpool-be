package kr.co.bookpool.app.campaign.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.co.bookpool.app.campaign.dto.request.CampaignSearchCondition;
import kr.co.bookpool.app.campaign.dto.request.SortKey;
import kr.co.bookpool.app.campaign.dto.response.CampaignResponse;
import kr.co.bookpool.app.campaign.dto.response.CategoryCountResponse;
import kr.co.bookpool.app.campaign.entity.Campaign;
import kr.co.bookpool.app.campaign.entity.CampaignCategory;
import kr.co.bookpool.app.campaign.entity.CampaignStatus;
import kr.co.bookpool.app.campaign.entity.PublishStatus;
import kr.co.bookpool.app.campaign.repository.CampaignRepository;
import kr.co.bookpool.app.campaign.repository.CampaignSpecifications;
import kr.co.bookpool.common.exception.BusinessException;
import kr.co.bookpool.common.exception.ErrorCode;
import kr.co.bookpool.common.response.PageResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignService {

	private static final int MAX_PAGE_SIZE = 200;

	private final CampaignRepository campaignRepository;

	public PageResponse<CampaignResponse> search(CampaignSearchCondition condition, int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

		// 공개 목록에는 검수 대기 중인 공고가 섞이면 안 된다.
		CampaignSearchCondition publicOnly = CampaignSearchCondition.builder()
			.query(condition.query())
			.publisher(condition.publisher())
			.categories(condition.categories())
			.types(condition.types())
			.deadline(condition.deadline())
			.withinDays(condition.withinDays())
			.from(condition.from())
			.to(condition.to())
			.dateBasis(condition.dateBasis())
			.publishStatus(PublishStatus.PUBLISHED)
			.sort(condition.sort())
			.build();

		Pageable pageable = PageRequest.of(safePage, safeSize, toSort(publicOnly.sort()));
		Specification<Campaign> spec = CampaignSpecifications.of(publicOnly);

		Page<CampaignResponse> result = campaignRepository.findAll(spec, pageable)
			.map(CampaignResponse::from);
		return PageResponse.from(result);
	}

	public CampaignResponse getById(Long campaignId) {
		return CampaignResponse.from(findPublished(campaignId));
	}

	@Transactional
	public CampaignResponse getAndIncreaseView(Long campaignId) {
		// 검수 대기 공고는 링크를 알아도 열리지 않아야 한다.
		findPublished(campaignId);

		// 동시 조회 시 조회수 갱신 유실을 막기 위해 원자적 UPDATE로 증가시킨다.
		int updated = campaignRepository.increaseViewCount(campaignId);
		if (updated == 0) {
			throw new BusinessException(ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		// clearAutomatically로 영속성 컨텍스트가 비워졌으므로 증가된 값을 다시 조회한다.
		return CampaignResponse.from(findPublished(campaignId));
	}

	public List<String> getAllPublishers() {
		return campaignRepository.findDistinctPublishers(PublishStatus.PUBLISHED);
	}

	/** 홈 카테고리 타일용 집계. 모집중이면서 게시된 공고만 센다. */
	public List<CategoryCountResponse> countByCategory() {
		List<CategoryCountResponse> counts = new ArrayList<>();
		for (Object[] row : campaignRepository.countByCategory(PublishStatus.PUBLISHED, CampaignStatus.OPEN)) {
			CampaignCategory category = (CampaignCategory)row[0];
			long count = (Long)row[1];
			// 프론트가 카테고리 키로 enum 이름을 쓴다.
			counts.add(new CategoryCountResponse(category.name(), count));
		}
		return counts;
	}

	public List<CampaignResponse> findByIds(List<Long> ids) {
		if (ids.isEmpty()) return List.of();
		return campaignRepository.findAllByIdInAndPublishStatus(ids, PublishStatus.PUBLISHED).stream()
			.map(CampaignResponse::from)
			.toList();
	}

	private Campaign findPublished(Long campaignId) {
		return campaignRepository.findByIdAndPublishStatus(campaignId, PublishStatus.PUBLISHED)
			.orElseThrow(() -> new BusinessException(ErrorCode.CAMPAIGN_NOT_FOUND));
	}

	private Sort toSort(SortKey sortKey) {
		return switch (sortKey) {
			case DEADLINE -> Sort.by(Sort.Direction.ASC, "deadlineAt");
			case VIEWS -> Sort.by(Sort.Direction.DESC, "viewCount");
			case LATEST -> Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
		};
	}
}
