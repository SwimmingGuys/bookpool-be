package kr.co.bookpool.app.inquiry.controller;

import static org.springframework.http.HttpStatus.*;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.co.bookpool.app.inquiry.controller.docs.AdminInquiryControllerDocs;
import kr.co.bookpool.app.inquiry.dto.request.AnswerInquiryRequest;
import kr.co.bookpool.app.inquiry.dto.response.InquiryResponse;
import kr.co.bookpool.app.inquiry.entity.InquiryStatus;
import kr.co.bookpool.app.inquiry.service.AdminInquiryService;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.response.PageResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/inquiries")
public class AdminInquiryController implements AdminInquiryControllerDocs {

	private final AdminInquiryService adminInquiryService;

	@Override
	@ResponseStatus(OK)
	@GetMapping
	public ApiResult<PageResponse<InquiryResponse>> list(
		@RequestParam(required = false) InquiryStatus status,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResult.success(adminInquiryService.list(status, page, size));
	}

	@Override
	@ResponseStatus(OK)
	@PostMapping("/{id}/answer")
	public ApiResult<InquiryResponse> answer(
		@PathVariable Long id,
		@Valid @RequestBody AnswerInquiryRequest request
	) {
		return ApiResult.success(adminInquiryService.answer(id, request));
	}
}
