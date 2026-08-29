package kr.co.bookpool.app.notification.controller;

import static org.springframework.http.HttpStatus.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import kr.co.bookpool.app.notification.controller.docs.NotificationControllerDocs;
import kr.co.bookpool.app.notification.dto.response.NotificationResponse;
import kr.co.bookpool.app.notification.service.NotificationService;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.response.PageResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me/notifications")
public class NotificationController implements NotificationControllerDocs {

	private final NotificationService notificationService;

	@Override
	@ResponseStatus(OK)
	@GetMapping
	public ApiResult<PageResponse<NotificationResponse>> listMine(
		@AuthenticationPrincipal Long memberId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResult.success(notificationService.listMine(memberId, page, size));
	}

	@Override
	@ResponseStatus(OK)
	@PostMapping("/{id}/read")
	public ApiResult<Void> markRead(
		@AuthenticationPrincipal Long memberId,
		@PathVariable Long id
	) {
		notificationService.markRead(memberId, id);
		return ApiResult.<Void>success(null);
	}

	@Override
	@ResponseStatus(OK)
	@PostMapping("/read-all")
	public ApiResult<Void> markAllRead(@AuthenticationPrincipal Long memberId) {
		notificationService.markAllRead(memberId);
		return ApiResult.<Void>success(null);
	}
}
