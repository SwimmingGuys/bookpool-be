package kr.co.bookpool.app.application.service;

import static kr.co.bookpool.common.exception.ErrorCode.*;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.co.bookpool.app.application.dto.response.ApplicationResponse;
import kr.co.bookpool.app.application.entity.Application;
import kr.co.bookpool.app.application.entity.ApplicationStatus;
import kr.co.bookpool.app.application.repository.ApplicationRepository;
import kr.co.bookpool.app.campaign.entity.Campaign;
import kr.co.bookpool.app.campaign.repository.CampaignRepository;
import kr.co.bookpool.app.member.entity.Member;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.common.exception.BusinessException;
import kr.co.bookpool.common.response.PageResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationService {

	private static final int MAX_PAGE_SIZE = 100;

	private final ApplicationRepository applicationRepository;
	private final CampaignRepository campaignRepository;
	private final MemberRepository memberRepository;

	/** 공고 카드·상세에서 '신청함'을 표시하기 위한 ID 목록. */
	public List<Long> getAppliedCampaignIds(Long memberId) {
		return applicationRepository.findCampaignIdsByMemberId(memberId);
	}

	public PageResponse<ApplicationResponse> list(
		Long memberId,
		ApplicationStatus status,
		int page,
		int size
	) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		// createdAt 동률 시 순서가 비결정적이 되지 않도록 id를 2차 정렬 키로 둔다.
		Sort sort = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
		Pageable pageable = PageRequest.of(safePage, safeSize, sort);

		Page<Application> result = status == null
			? applicationRepository.findAllByMemberIdWithCampaign(memberId, pageable)
			: applicationRepository.findAllByMemberIdAndStatusWithCampaign(memberId, status, pageable);

		return PageResponse.from(result.map(ApplicationResponse::from));
	}

	@Transactional
	public ApplicationResponse apply(Long memberId, Long campaignId, ApplicationStatus status) {
		if (applicationRepository.existsByMemberIdAndCampaignId(memberId, campaignId)) {
			throw new BusinessException(APPLICATION_ALREADY_EXISTS);
		}
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(MEMBER_NOT_FOUND));
		Campaign campaign = campaignRepository.findById(campaignId)
			.orElseThrow(() -> new BusinessException(CAMPAIGN_NOT_FOUND));

		Application saved = applicationRepository.save(Application.create(member, campaign, status));
		return ApplicationResponse.from(saved);
	}

	/** 발표 결과를 사용자가 직접 표시한다(당첨/미당첨). */
	@Transactional
	public ApplicationResponse changeStatus(Long memberId, Long campaignId, ApplicationStatus status) {
		Application application = applicationRepository.findByMemberIdAndCampaignId(memberId, campaignId)
			.orElseThrow(() -> new BusinessException(APPLICATION_NOT_FOUND));
		application.changeStatus(status);
		return ApplicationResponse.from(application);
	}

	@Transactional
	public void cancel(Long memberId, Long campaignId) {
		long deleted = applicationRepository.deleteByMemberIdAndCampaignId(memberId, campaignId);
		if (deleted == 0) {
			throw new BusinessException(APPLICATION_NOT_FOUND);
		}
	}
}
