package kr.co.bookpool.app.notification.service;

import static kr.co.bookpool.common.exception.ErrorCode.*;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.co.bookpool.app.notification.dto.response.NotificationResponse;
import kr.co.bookpool.app.notification.entity.Notification;
import kr.co.bookpool.app.notification.repository.NotificationRepository;
import kr.co.bookpool.common.exception.BusinessException;
import kr.co.bookpool.common.response.PageResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

	private static final int MAX_PAGE_SIZE = 100;

	private final NotificationRepository notificationRepository;

	public PageResponse<NotificationResponse> listMine(Long memberId, int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
		return PageResponse.from(
			notificationRepository.findAllByMemberId(memberId, pageable).map(NotificationResponse::from)
		);
	}

	@Transactional
	public void markRead(Long memberId, Long notificationId) {
		Notification notification = notificationRepository.findWithCampaignById(notificationId)
			.orElseThrow(() -> new BusinessException(NOTIFICATION_NOT_FOUND));
		// 남의 알림을 읽음 처리하지 못하게 한다. 존재 여부도 알려주지 않도록 같은 코드로 응답한다.
		if (!notification.isOwnedBy(memberId)) {
			throw new BusinessException(NOTIFICATION_NOT_FOUND);
		}
		notification.markRead();
	}

	@Transactional
	public void markAllRead(Long memberId) {
		notificationRepository.markAllRead(memberId);
	}
}
