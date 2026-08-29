package kr.co.bookpool.app.review.controller;

import static org.springframework.http.HttpStatus.*;

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
import kr.co.bookpool.app.review.controller.docs.AdminReviewControllerDocs;
import kr.co.bookpool.app.review.dto.request.ReviewDecisionRequest;
import kr.co.bookpool.app.review.dto.request.ReviewStatusRequest;
import kr.co.bookpool.app.review.dto.response.ReviewResponse;
import kr.co.bookpool.app.review.entity.ReviewSubmissionStatus;
import kr.co.bookpool.app.review.service.AdminReviewService;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.response.PageResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/reviews")
public class AdminReviewController implements AdminReviewControllerDocs {

	private final AdminReviewService adminReviewService;

	@Override
	@ResponseStatus(OK)
	@GetMapping
	public ApiResult<PageResponse<ReviewResponse>> list(
		@RequestParam(required = false) Long campaignId,
		@RequestParam(required = false) ReviewSubmissionStatus submissionStatus,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResult.success(adminReviewService.list(campaignId, submissionStatus, page, size));
	}

	@Override
	@ResponseStatus(OK)
	@PostMapping("/{id}/decision")
	public ApiResult<ReviewResponse> decide(
		@PathVariable Long id,
		@Valid @RequestBody ReviewDecisionRequest request
	) {
		return ApiResult.success(adminReviewService.decide(id, request));
	}

	@Override
	@ResponseStatus(OK)
	@PatchMapping("/{id}/status")
	public ApiResult<ReviewResponse> changeStatus(
		@PathVariable Long id,
		@Valid @RequestBody ReviewStatusRequest request
	) {
		return ApiResult.success(adminReviewService.changeStatus(id, request.status()));
	}

	@Override
	@ResponseStatus(OK)
	@DeleteMapping("/{id}")
	public ApiResult<Void> delete(@PathVariable Long id) {
		adminReviewService.delete(id);
		return ApiResult.<Void>success(null);
	}
}
