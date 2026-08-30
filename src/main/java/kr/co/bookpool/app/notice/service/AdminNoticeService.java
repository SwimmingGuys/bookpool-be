package kr.co.bookpool.app.notice.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.co.bookpool.app.member.entity.Member;
import kr.co.bookpool.app.member.repository.MemberRepository;
import kr.co.bookpool.app.notice.dto.request.NoticeRequest;
import kr.co.bookpool.app.notice.dto.response.NoticeResponse;
import kr.co.bookpool.app.notice.entity.Notice;
import kr.co.bookpool.app.notice.repository.NoticeRepository;
import kr.co.bookpool.common.exception.BusinessException;
import kr.co.bookpool.common.exception.ErrorCode;
import kr.co.bookpool.common.response.PageResponse;
import lombok.RequiredArgsConstructor;

/**
 * 백오피스 공지 관리.
 * 사용자 화면에는 공지 목록이 있었지만 쓸 수 있는 화면이 없었다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminNoticeService {

	private static final int MAX_PAGE_SIZE = 100;

	private final NoticeRepository noticeRepository;
	private final MemberRepository memberRepository;

	public PageResponse<NoticeResponse> list(int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		Sort sort = Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("createdAt"));
		Pageable pageable = PageRequest.of(safePage, safeSize, sort);
		return PageResponse.from(noticeRepository.findAll(pageable).map(NoticeResponse::from));
	}

	@Transactional
	public NoticeResponse create(Long adminId, NoticeRequest request) {
		Member admin = memberRepository.findById(adminId)
			.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
		Notice saved = noticeRepository.save(Notice.create(
			admin, request.title().trim(), request.content().trim(), request.category(), request.isPinned()
		));
		return NoticeResponse.from(saved);
	}

	@Transactional
	public NoticeResponse update(Long noticeId, NoticeRequest request) {
		Notice notice = findById(noticeId);
		notice.update(
			request.title().trim(), request.content().trim(), request.category(), request.isPinned()
		);
		return NoticeResponse.from(notice);
	}

	@Transactional
	public void delete(Long noticeId) {
		noticeRepository.delete(findById(noticeId));
	}

	private Notice findById(Long noticeId) {
		return noticeRepository.findById(noticeId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));
	}
}
