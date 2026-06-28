package kr.co.bookpool.app.campaign.service;

import static kr.co.bookpool.common.exception.ErrorCode.*;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.co.bookpool.app.campaign.dto.request.CampaignCreateRequest;
import kr.co.bookpool.app.campaign.dto.request.CampaignUpdateRequest;
import kr.co.bookpool.app.campaign.dto.response.CampaignResponse;
import kr.co.bookpool.app.campaign.dto.response.PageResponse;
import kr.co.bookpool.app.campaign.entity.Campaign;
import kr.co.bookpool.app.campaign.repository.CampaignRepository;
import kr.co.bookpool.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCampaignService {

	private static final int MAX_PAGE_SIZE = 100;

	private final CampaignRepository campaignRepository;

	public PageResponse<CampaignResponse> list(int page, int size) {
		// 잘못된 페이지 파라미터로 PageRequest가 예외(→500)를 던지지 않도록 방어적으로 보정한다.
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		// 관리자 목록은 상태와 무관하게 최신 등록순으로 전부 조회한다. createdAt 동률 시 id를 2차 키로 둬 순서를 안정화.
		Sort sort = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
		Pageable pageable = PageRequest.of(safePage, safeSize, sort);
		return PageResponse.from(campaignRepository.findAll(pageable).map(CampaignResponse::from));
	}

	public CampaignResponse get(Long campaignId) {
		return CampaignResponse.from(findById(campaignId));
	}

	@Transactional
	public CampaignResponse create(CampaignCreateRequest request) {
		Campaign saved = campaignRepository.save(request.toEntity());
		return CampaignResponse.from(saved);
	}

	@Transactional
	public CampaignResponse update(Long campaignId, CampaignUpdateRequest request) {
		Campaign campaign = findById(campaignId);
		campaign.update(
			request.title(), request.bookTitle(), request.publisherName(), request.category(),
			request.type(), request.applyUrl(), request.imageUrl(), request.description(),
			request.recruitStartDate(), request.deadlineAt(), request.announcementDate(), request.status()
		);
		return CampaignResponse.from(campaign);
	}

	@Transactional
	public void delete(Long campaignId) {
		campaignRepository.delete(findById(campaignId));
	}

	private Campaign findById(Long campaignId) {
		return campaignRepository.findById(campaignId)
			.orElseThrow(() -> new BusinessException(CAMPAIGN_NOT_FOUND));
	}
}
