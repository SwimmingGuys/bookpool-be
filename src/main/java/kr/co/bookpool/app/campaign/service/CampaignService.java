package kr.co.bookpool.app.campaign.service;

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
import kr.co.bookpool.app.campaign.dto.response.PageResponse;
import kr.co.bookpool.app.campaign.entity.Campaign;
import kr.co.bookpool.app.campaign.repository.CampaignRepository;
import kr.co.bookpool.app.campaign.repository.CampaignSpecifications;
import kr.co.bookpool.common.exception.BusinessException;
import kr.co.bookpool.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignService {

	private final CampaignRepository campaignRepository;

	public PageResponse<CampaignResponse> search(CampaignSearchCondition condition, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, toSort(condition.sort()));
		Specification<Campaign> spec = CampaignSpecifications.of(condition);

		Page<CampaignResponse> result = campaignRepository.findAll(spec, pageable)
			.map(CampaignResponse::from);
		return PageResponse.from(result);
	}

	public CampaignResponse getById(Long campaignId) {
		Campaign campaign = campaignRepository.findById(campaignId)
			.orElseThrow(() -> new BusinessException(ErrorCode.CAMPAIGN_NOT_FOUND));
		return CampaignResponse.from(campaign);
	}

	@Transactional
	public CampaignResponse getAndIncreaseView(Long campaignId) {
		// 동시 조회 시 조회수 갱신 유실을 막기 위해 원자적 UPDATE로 증가시킨다.
		int updated = campaignRepository.increaseViewCount(campaignId);
		if (updated == 0) {
			throw new BusinessException(ErrorCode.CAMPAIGN_NOT_FOUND);
		}
		// clearAutomatically로 영속성 컨텍스트가 비워졌으므로 증가된 값을 다시 조회한다.
		Campaign campaign = campaignRepository.findById(campaignId)
			.orElseThrow(() -> new BusinessException(ErrorCode.CAMPAIGN_NOT_FOUND));
		return CampaignResponse.from(campaign);
	}

	public List<String> getAllPublishers() {
		return campaignRepository.findAll().stream()
			.map(Campaign::getPublisherName)
			.distinct()
			.sorted()
			.toList();
	}

	public List<CampaignResponse> findByIds(List<Long> ids) {
		if (ids.isEmpty()) return List.of();
		return campaignRepository.findAllByIdIn(ids).stream()
			.map(CampaignResponse::from)
			.toList();
	}

	private Sort toSort(SortKey sortKey) {
		return switch (sortKey) {
			case DEADLINE -> Sort.by(Sort.Direction.ASC, "deadlineAt");
			case POPULAR -> Sort.by(Sort.Direction.DESC, "viewCount");
		};
	}
}
