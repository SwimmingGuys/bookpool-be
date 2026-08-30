package kr.co.bookpool.app.application.controller;

import static org.springframework.http.HttpStatus.*;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.co.bookpool.app.application.controller.docs.ApplicationControllerDocs;
import kr.co.bookpool.app.application.dto.request.ApplicationRequest;
import kr.co.bookpool.app.application.dto.request.ApplicationStatusRequest;
import kr.co.bookpool.app.application.dto.response.ApplicationResponse;
import kr.co.bookpool.app.application.entity.ApplicationStatus;
import kr.co.bookpool.app.application.service.ApplicationService;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.response.PageResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me/applications")
public class ApplicationController implements ApplicationControllerDocs {

	private final ApplicationService applicationService;

	@Override
	@ResponseStatus(OK)
	@GetMapping("/ids")
	public ApiResult<List<Long>> getAppliedCampaignIds(@AuthenticationPrincipal Long memberId) {
		return ApiResult.success(applicationService.getAppliedCampaignIds(memberId));
	}

	@Override
	@ResponseStatus(OK)
	@GetMapping
	public ApiResult<PageResponse<ApplicationResponse>> list(
		@AuthenticationPrincipal Long memberId,
		@RequestParam(required = false) ApplicationStatus status,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResult.success(applicationService.list(memberId, status, page, size));
	}

	@Override
	@ResponseStatus(CREATED)
	@PostMapping
	public ApiResult<ApplicationResponse> apply(
		@AuthenticationPrincipal Long memberId,
		@Valid @RequestBody ApplicationRequest request
	) {
		return ApiResult.success(
			applicationService.apply(memberId, request.campaignId(), request.status())
		);
	}

	@Override
	@ResponseStatus(OK)
	@PatchMapping("/{campaignId}")
	public ApiResult<ApplicationResponse> changeStatus(
		@AuthenticationPrincipal Long memberId,
		@PathVariable Long campaignId,
		@Valid @RequestBody ApplicationStatusRequest request
	) {
		return ApiResult.success(
			applicationService.changeStatus(memberId, campaignId, request.status())
		);
	}

	@Override
	@ResponseStatus(OK)
	@DeleteMapping("/{campaignId}")
	public ApiResult<Void> cancel(
		@AuthenticationPrincipal Long memberId,
		@PathVariable Long campaignId
	) {
		applicationService.cancel(memberId, campaignId);
		return ApiResult.<Void>success(null);
	}
}
