package kr.co.bookpool.app.notification.controller;

import static org.springframework.http.HttpStatus.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.co.bookpool.app.notification.controller.docs.NotificationSubscriptionControllerDocs;
import kr.co.bookpool.app.notification.dto.NotificationSubscriptionDto;
import kr.co.bookpool.app.notification.service.NotificationSubscriptionService;
import kr.co.bookpool.common.response.ApiResult;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me/notification-subscription")
public class NotificationSubscriptionController implements NotificationSubscriptionControllerDocs {

	private final NotificationSubscriptionService subscriptionService;

	@Override
	@ResponseStatus(OK)
	@GetMapping
	public ApiResult<NotificationSubscriptionDto> get(@AuthenticationPrincipal Long memberId) {
		return ApiResult.success(subscriptionService.get(memberId));
	}

	@Override
	@ResponseStatus(OK)
	@PutMapping
	public ApiResult<NotificationSubscriptionDto> save(
		@AuthenticationPrincipal Long memberId,
		@Valid @RequestBody NotificationSubscriptionDto request
	) {
		return ApiResult.success(subscriptionService.save(memberId, request));
	}
}
