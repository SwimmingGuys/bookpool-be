package kr.co.bookpool.app.inquiry.service;

import static kr.co.bookpool.common.exception.ErrorCode.*;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.co.bookpool.app.inquiry.dto.request.CreateInquiryRequest;
import kr.co.bookpool.app.inquiry.dto.response.InquiryResponse;
import kr.co.bookpool.app.inquiry.entity.Inquiry;
import kr.co.bookpool.app.inquiry.repository.InquiryRepository;
import kr.co.bookpool.app.member.entity.Member;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.common.exception.BusinessException;
import kr.co.bookpool.common.response.PageResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

	private static final int MAX_PAGE_SIZE = 100;

	private final InquiryRepository inquiryRepository;
	private final MemberRepository memberRepository;

	public PageResponse<InquiryResponse> listMine(Long memberId, int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
		return PageResponse.from(
			inquiryRepository.findAllByMemberId(memberId, pageable).map(InquiryResponse::from)
		);
	}

	public InquiryResponse getMine(Long memberId, Long inquiryId) {
		Inquiry inquiry = inquiryRepository.findByIdAndMemberId(inquiryId, memberId)
			.orElseThrow(() -> new BusinessException(INQUIRY_NOT_FOUND));
		return InquiryResponse.from(inquiry);
	}

	@Transactional
	public InquiryResponse create(Long memberId, CreateInquiryRequest request) {
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new BusinessException(MEMBER_NOT_FOUND));
		Inquiry saved = inquiryRepository.save(
			Inquiry.create(member, request.type(), request.title().trim(), request.content().trim())
		);
		return InquiryResponse.from(saved);
	}
}
