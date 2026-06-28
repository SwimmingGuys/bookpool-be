package kr.co.bookpool.app.recentview.controller;

import static org.springframework.http.HttpStatus.*;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.co.bookpool.app.campaign.dto.response.CampaignResponse;
import kr.co.bookpool.app.campaign.dto.response.PageResponse;
import kr.co.bookpool.app.recentview.controller.docs.RecentViewControllerDocs;
import kr.co.bookpool.app.recentview.dto.request.RecentViewRequest;
import kr.co.bookpool.app.recentview.service.RecentViewService;
import kr.co.bookpool.common.response.ApiResult;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me/recent-views")
public class RecentViewController implements RecentViewControllerDocs {

	private final RecentViewService recentViewService;

	@Override
	@ResponseStatus(OK)
	@GetMapping("/ids")
	public ApiResult<List<Long>> ids(
		@AuthenticationPrincipal Long memberId,
		@RequestParam(defaultValue = "50") int limit
	) {
		return ApiResult.success(recentViewService.getRecentIds(memberId, limit));
	}

	@Override
	@ResponseStatus(OK)
	@GetMapping
	public ApiResult<PageResponse<CampaignResponse>> list(
		@AuthenticationPrincipal Long memberId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResult.success(recentViewService.getRecentCampaigns(memberId, page, size));
	}

	@Override
	@ResponseStatus(OK)
	@PostMapping
	public ApiResult<Void> mark(
		@AuthenticationPrincipal Long memberId,
		@Valid @RequestBody RecentViewRequest request
	) {
		recentViewService.mark(memberId, request.campaignId());
		return ApiResult.<Void>success(null);
	}
}
