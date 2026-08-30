package kr.co.bookpool.app.review.service;

import static kr.co.bookpool.common.exception.ErrorCode.*;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.co.bookpool.app.campaign.entity.Campaign;
import kr.co.bookpool.app.campaign.repository.CampaignRepository;
import kr.co.bookpool.app.member.entity.Member;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.app.review.dto.request.ReviewRequest;
import kr.co.bookpool.app.review.dto.response.ReviewResponse;
import kr.co.bookpool.app.review.entity.Review;
import kr.co.bookpool.app.review.entity.ReviewStatus;
import kr.co.bookpool.app.review.repository.ReviewRepository;
import kr.co.bookpool.common.exception.BusinessException;
import kr.co.bookpool.common.response.PageResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

	private static final int MAX_PAGE_SIZE = 100;

	private final ReviewRepository reviewRepository;
	private final CampaignRepository campaignRepository;
	private final MemberRepository memberRepository;

	/**
	 * 공고 상세에 노출되는 서평.
	 * 관리자가 인증(APPROVED)하고 숨기지 않은(VISIBLE) 것만 보인다.
	 */
	public PageResponse<ReviewResponse> listByCampaign(Long campaignId, Long viewerId, int page, int size) {
		Pageable pageable = pageable(page, size);
		// 사전 승인 없이 바로 노출한다(사후 검열).
		// 관리자 승인을 거쳐야만 보이게 두면, 승인하는 사람이 상주하지 않는 한 후기가
		// 영영 화면에 뜨지 않아 남긴 사람에게도 흔적이 남지 않는다.
		// 부적절한 후기는 관리자가 status를 HIDDEN으로 내려 가린다.
		// submissionStatus(확인 대기/인증 완료)는 이제 노출 여부가 아니라,
		// 서평 원문 링크를 관리자가 확인해 줬는지를 나타내는 표시로만 쓴다.
		return PageResponse.from(
			reviewRepository.findAllByCampaignIdAndStatus(campaignId, ReviewStatus.VISIBLE, pageable)
				.map(review -> ReviewResponse.of(review, viewerId))
		);
	}

	/** 마이페이지의 '내 서평'. 확인 대기·반려된 것도 본인은 볼 수 있다. */
	public PageResponse<ReviewResponse> listMine(Long memberId, int page, int size) {
		return PageResponse.from(
			reviewRepository.findAllByMemberId(memberId, pageable(page, size))
				.map(review -> ReviewResponse.of(review, memberId))
		);
	}

	@Transactional
	public ReviewResponse submit(Long memberId, ReviewRequest request) {
		if (request.campaignId() == null) {
			throw new BusinessException(INVALID_INPUT_VALUE);
		}
		Campaign campaign = campaignRepository.findById(request.campaignId())
			.orElseThrow(() -> new BusinessException(CAMPAIGN_NOT_FOUND));

		// 한 공고에 한 번만. DB 유니크 제약과 함께 이중으로 막는다.
		if (reviewRepository.existsByCampaignIdAndMemberId(campaign.getId(), memberId)) {
			throw new BusinessException(REVIEW_ALREADY_EXISTS);
		}

		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(MEMBER_NOT_FOUND));

		Review saved = reviewRepository.save(Review.create(
			campaign, member, request.rating(), request.content().trim(),
			request.channel(), request.url().trim()
		));
		return ReviewResponse.of(saved, memberId);
	}

	@Transactional
	public ReviewResponse update(Long memberId, Long reviewId, ReviewRequest request) {
		Review review = findOwned(memberId, reviewId);
		review.update(request.rating(), request.content().trim(), request.channel(), request.url().trim());
		return ReviewResponse.of(review, memberId);
	}

	@Transactional
	public void delete(Long memberId, Long reviewId) {
		reviewRepository.delete(findOwned(memberId, reviewId));
	}

	private Review findOwned(Long memberId, Long reviewId) {
		Review review = reviewRepository.findWithCampaignById(reviewId)
			.orElseThrow(() -> new BusinessException(REVIEW_NOT_FOUND));
		if (!review.isOwnedBy(memberId)) {
			throw new BusinessException(REVIEW_FORBIDDEN);
		}
		return review;
	}

	private Pageable pageable(int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
	}
}
