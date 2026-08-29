package kr.co.bookpool.app.review.service;

import static kr.co.bookpool.common.exception.ErrorCode.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.co.bookpool.app.review.dto.request.ReviewDecisionRequest;
import kr.co.bookpool.app.review.dto.response.ReviewResponse;
import kr.co.bookpool.app.review.entity.Review;
import kr.co.bookpool.app.review.entity.ReviewStatus;
import kr.co.bookpool.app.review.entity.ReviewSubmissionStatus;
import kr.co.bookpool.app.review.repository.ReviewRepository;
import kr.co.bookpool.common.exception.BusinessException;
import kr.co.bookpool.common.response.PageResponse;
import lombok.RequiredArgsConstructor;

/** 백오피스 서평 인증. 제출된 서평을 승인·반려하고 노출 여부를 정한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReviewService {

	private static final int MAX_PAGE_SIZE = 100;

	private final ReviewRepository reviewRepository;

	public PageResponse<ReviewResponse> list(
		Long campaignId,
		ReviewSubmissionStatus submissionStatus,
		int page,
		int size
	) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		// 확인해야 할 것이 위로 오도록 오래된 순.
		Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "createdAt"));

		Page<Review> result;
		if (campaignId != null && submissionStatus != null) {
			result = reviewRepository.findAllByCampaignIdAndSubmissionStatus(campaignId, submissionStatus, pageable);
		} else if (campaignId != null) {
			result = reviewRepository.findAllByCampaignId(campaignId, pageable);
		} else if (submissionStatus != null) {
			result = reviewRepository.findAllBySubmissionStatus(submissionStatus, pageable);
		} else {
			result = reviewRepository.findAll(pageable);
		}

		// 관리자 화면에는 '내 서평' 표시가 필요 없다.
		return PageResponse.from(result.map(review -> ReviewResponse.of(review, null)));
	}

	@Transactional
	public ReviewResponse decide(Long reviewId, ReviewDecisionRequest request) {
		Review review = findById(reviewId);
		if (request.submissionStatus() == ReviewSubmissionStatus.APPROVED) {
			review.approve();
		} else if (request.submissionStatus() == ReviewSubmissionStatus.REJECTED) {
			// 반려는 사유가 있어야 참여자가 무엇을 고쳐야 하는지 안다.
			if (request.rejectReason() == null || request.rejectReason().isBlank()) {
				throw new BusinessException(INVALID_INPUT_VALUE);
			}
			review.reject(request.rejectReason().trim());
		} else {
			throw new BusinessException(INVALID_INPUT_VALUE);
		}
		return ReviewResponse.of(review, null);
	}

	@Transactional
	public ReviewResponse changeStatus(Long reviewId, ReviewStatus status) {
		Review review = findById(reviewId);
		review.changeStatus(status);
		return ReviewResponse.of(review, null);
	}

	@Transactional
	public void delete(Long reviewId) {
		reviewRepository.delete(findById(reviewId));
	}

	private Review findById(Long reviewId) {
		return reviewRepository.findWithCampaignById(reviewId)
			.orElseThrow(() -> new BusinessException(REVIEW_NOT_FOUND));
	}
}
