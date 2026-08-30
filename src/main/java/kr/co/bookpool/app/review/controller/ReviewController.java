package kr.co.bookpool.app.review.controller;

import static org.springframework.http.HttpStatus.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.co.bookpool.app.review.controller.docs.ReviewControllerDocs;
import kr.co.bookpool.app.review.dto.request.ReviewRequest;
import kr.co.bookpool.app.review.dto.response.ReviewResponse;
import kr.co.bookpool.app.review.service.ReviewService;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.response.PageResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController implements ReviewControllerDocs {

	private final ReviewService reviewService;

	@Override
	@ResponseStatus(OK)
	@GetMapping
	public ApiResult<PageResponse<ReviewResponse>> listByCampaign(
		// 비로그인도 볼 수 있는 목록이라 principal은 null일 수 있다.
		@AuthenticationPrincipal Long memberId,
		@RequestParam Long campaignId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResult.success(reviewService.listByCampaign(campaignId, memberId, page, size));
	}

	@Override
	@ResponseStatus(OK)
	@GetMapping("/me")
	public ApiResult<PageResponse<ReviewResponse>> listMine(
		@AuthenticationPrincipal Long memberId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResult.success(reviewService.listMine(memberId, page, size));
	}

	@Override
	@ResponseStatus(CREATED)
	@PostMapping
	public ApiResult<ReviewResponse> submit(
		@AuthenticationPrincipal Long memberId,
		@Valid @RequestBody ReviewRequest request
	) {
		return ApiResult.success(reviewService.submit(memberId, request));
	}

	@Override
	@ResponseStatus(OK)
	@PutMapping("/{id}")
	public ApiResult<ReviewResponse> update(
		@AuthenticationPrincipal Long memberId,
		@PathVariable Long id,
		@Valid @RequestBody ReviewRequest request
	) {
		return ApiResult.success(reviewService.update(memberId, id, request));
	}

	@Override
	@ResponseStatus(OK)
	@DeleteMapping("/{id}")
	public ApiResult<Void> delete(
		@AuthenticationPrincipal Long memberId,
		@PathVariable Long id
	) {
		reviewService.delete(memberId, id);
		return ApiResult.<Void>success(null);
	}
}
