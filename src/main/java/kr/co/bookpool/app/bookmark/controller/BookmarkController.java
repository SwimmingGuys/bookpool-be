package kr.co.bookpool.app.bookmark.controller;

import static org.springframework.http.HttpStatus.*;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.co.bookpool.app.bookmark.controller.docs.BookmarkControllerDocs;
import kr.co.bookpool.app.bookmark.dto.request.BookmarkRequest;
import kr.co.bookpool.app.bookmark.service.BookmarkService;
import kr.co.bookpool.app.campaign.dto.response.CampaignResponse;
import kr.co.bookpool.common.response.PageResponse;
import kr.co.bookpool.common.response.ApiResult;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me/bookmarks")
public class BookmarkController implements BookmarkControllerDocs {

	private final BookmarkService bookmarkService;

	@Override
	@ResponseStatus(OK)
	@GetMapping("/ids")
	public ApiResult<List<Long>> getBookmarkedCampaignIds(@AuthenticationPrincipal Long memberId) {
		return ApiResult.success(bookmarkService.getBookmarkedCampaignIds(memberId));
	}

	@Override
	@ResponseStatus(OK)
	@GetMapping
	public ApiResult<PageResponse<CampaignResponse>> getBookmarkedCampaigns(
		@AuthenticationPrincipal Long memberId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResult.success(bookmarkService.getBookmarkedCampaigns(memberId, page, size));
	}

	@Override
	@ResponseStatus(CREATED)
	@PostMapping
	public ApiResult<Void> add(
		@AuthenticationPrincipal Long memberId,
		@Valid @RequestBody BookmarkRequest request
	) {
		bookmarkService.add(memberId, request.campaignId());
		return ApiResult.<Void>success(null);
	}

	@Override
	@ResponseStatus(OK)
	@DeleteMapping("/{campaignId}")
	public ApiResult<Void> remove(
		@AuthenticationPrincipal Long memberId,
		@PathVariable Long campaignId
	) {
		bookmarkService.remove(memberId, campaignId);
		return ApiResult.<Void>success(null);
	}
}
