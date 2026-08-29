package kr.co.bookpool.app.inquiry.service;

import static kr.co.bookpool.common.exception.ErrorCode.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.co.bookpool.app.inquiry.dto.request.AnswerInquiryRequest;
import kr.co.bookpool.app.inquiry.dto.response.InquiryResponse;
import kr.co.bookpool.app.inquiry.entity.Inquiry;
import kr.co.bookpool.app.inquiry.entity.InquiryStatus;
import kr.co.bookpool.app.inquiry.repository.InquiryRepository;
import kr.co.bookpool.common.exception.BusinessException;
import kr.co.bookpool.common.response.PageResponse;
import lombok.RequiredArgsConstructor;

/**
 * 백오피스 문의 답변.
 * 사용자는 문의를 남길 수 있었지만 받을 화면이 없었다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminInquiryService {

	private static final int MAX_PAGE_SIZE = 100;

	private final InquiryRepository inquiryRepository;

	public PageResponse<InquiryResponse> list(InquiryStatus status, int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		// 미답변을 먼저 처리하는 흐름이라 오래된 문의가 위로 오도록 오름차순으로 둔다.
		Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "createdAt"));

		Page<Inquiry> result = status == null
			? inquiryRepository.findAll(pageable)
			: inquiryRepository.findAllByStatus(status, pageable);

		return PageResponse.from(result.map(InquiryResponse::forAdmin));
	}

	/** 답변을 등록하거나 이미 등록된 답변을 고친다. */
	@Transactional
	public InquiryResponse answer(Long inquiryId, AnswerInquiryRequest request) {
		Inquiry inquiry = inquiryRepository.findById(inquiryId)
			.orElseThrow(() -> new BusinessException(INQUIRY_NOT_FOUND));
		inquiry.answer(request.answer().trim());
		return InquiryResponse.forAdmin(inquiry);
	}
}
