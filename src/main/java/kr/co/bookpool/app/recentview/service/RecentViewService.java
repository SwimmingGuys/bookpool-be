package kr.co.bookpool.app.recentview.service;

import static kr.co.bookpool.common.exception.ErrorCode.*;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.co.bookpool.app.campaign.dto.response.CampaignResponse;
import kr.co.bookpool.common.response.PageResponse;
import kr.co.bookpool.app.campaign.entity.Campaign;
import kr.co.bookpool.app.campaign.repository.CampaignRepository;
import kr.co.bookpool.app.member.entity.Member;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.app.recentview.entity.RecentView;
import kr.co.bookpool.app.recentview.repository.RecentViewRepository;
import kr.co.bookpool.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecentViewService {

	private static final int MAX_LIMIT = 50;
	private static final int MAX_PAGE_SIZE = 100;

	private final RecentViewRepository recentViewRepository;
	private final CampaignRepository campaignRepository;
	private final MemberRepository memberRepository;

	public List<Long> getRecentIds(Long memberId, int limit) {
		int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
		Pageable pageable = PageRequest.of(0, safeLimit);
		return recentViewRepository.findRecentCampaignIds(memberId, pageable);
	}

	public PageResponse<CampaignResponse> getRecentCampaigns(Long memberId, int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		// 최근 본 순(viewedAt desc), 동률 시 id desc로 안정 정렬한다.
		Sort sort = Sort.by(Sort.Direction.DESC, "viewedAt").and(Sort.by(Sort.Direction.DESC, "id"));
		Pageable pageable = PageRequest.of(safePage, safeSize, sort);
		return PageResponse.from(
			recentViewRepository.findAllByMemberIdWithCampaign(memberId, pageable)
				.map(recentView -> CampaignResponse.from(recentView.getCampaign()))
		);
	}

	/** 캠페인 열람을 기록한다. 이미 있으면 열람 시각만 갱신(멱등). */
	@Transactional
	public void mark(Long memberId, Long campaignId) {
		recentViewRepository.findByMemberIdAndCampaignId(memberId, campaignId)
			.ifPresentOrElse(
				RecentView::touch,
				() -> {
					Member member = memberRepository.findById(memberId)
						.orElseThrow(() -> new BusinessException(MEMBER_NOT_FOUND));
					Campaign campaign = campaignRepository.findById(campaignId)
						.orElseThrow(() -> new BusinessException(CAMPAIGN_NOT_FOUND));
					recentViewRepository.save(RecentView.create(member, campaign));
				}
			);
	}
}
