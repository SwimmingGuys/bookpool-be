package kr.co.bookpool.app.inquiry.controller;

import static org.springframework.http.HttpStatus.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.co.bookpool.app.inquiry.controller.docs.InquiryControllerDocs;
import kr.co.bookpool.app.inquiry.dto.request.CreateInquiryRequest;
import kr.co.bookpool.app.inquiry.dto.response.InquiryResponse;
import kr.co.bookpool.app.inquiry.service.InquiryService;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.response.PageResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inquiries")
public class InquiryController implements InquiryControllerDocs {

	private final InquiryService inquiryService;

	@Override
	@ResponseStatus(OK)
	@GetMapping
	public ApiResult<PageResponse<InquiryResponse>> listMine(
		@AuthenticationPrincipal Long memberId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResult.success(inquiryService.listMine(memberId, page, size));
	}

	@Override
	@ResponseStatus(OK)
	@GetMapping("/{id}")
	public ApiResult<InquiryResponse> detail(
		@AuthenticationPrincipal Long memberId,
		@PathVariable Long id
	) {
		return ApiResult.success(inquiryService.getMine(memberId, id));
	}

	@Override
	@ResponseStatus(CREATED)
	@PostMapping
	public ApiResult<InquiryResponse> create(
		@AuthenticationPrincipal Long memberId,
		@Valid @RequestBody CreateInquiryRequest request
	) {
		return ApiResult.success(inquiryService.create(memberId, request));
	}
}
