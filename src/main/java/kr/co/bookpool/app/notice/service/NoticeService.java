package kr.co.bookpool.app.notice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.co.bookpool.common.response.PageResponse;
import kr.co.bookpool.app.notice.dto.response.NoticeResponse;
import kr.co.bookpool.app.notice.entity.Notice;
import kr.co.bookpool.app.notice.entity.NoticeCategory;
import kr.co.bookpool.app.notice.repository.NoticeRepository;
import kr.co.bookpool.common.exception.BusinessException;
import kr.co.bookpool.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

	private final NoticeRepository noticeRepository;

	public PageResponse<NoticeResponse> list(NoticeCategory category, int page, int size) {
		Sort sort = Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("createdAt"));
		Pageable pageable = PageRequest.of(page, size, sort);

		Page<Notice> result = category == null
			? noticeRepository.findAll(pageable)
			: noticeRepository.findAllByCategory(category, pageable);

		return PageResponse.from(result.map(NoticeResponse::from));
	}

	public NoticeResponse detail(Long id) {
		Notice notice = noticeRepository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));
		return NoticeResponse.from(notice);
	}
}
