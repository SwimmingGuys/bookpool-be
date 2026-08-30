package kr.co.bookpool.app.notice.controller;

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
import kr.co.bookpool.app.notice.controller.docs.AdminNoticeControllerDocs;
import kr.co.bookpool.app.notice.dto.request.NoticeRequest;
import kr.co.bookpool.app.notice.dto.response.NoticeResponse;
import kr.co.bookpool.app.notice.service.AdminNoticeService;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.response.PageResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notices")
public class AdminNoticeController implements AdminNoticeControllerDocs {

	private final AdminNoticeService adminNoticeService;

	@Override
	@ResponseStatus(OK)
	@GetMapping
	public ApiResult<PageResponse<NoticeResponse>> list(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResult.success(adminNoticeService.list(page, size));
	}

	@Override
	@ResponseStatus(CREATED)
	@PostMapping
	public ApiResult<NoticeResponse> create(
		@AuthenticationPrincipal Long adminId,
		@Valid @RequestBody NoticeRequest request
	) {
		return ApiResult.success(adminNoticeService.create(adminId, request));
	}

	@Override
	@ResponseStatus(OK)
	@PutMapping("/{id}")
	public ApiResult<NoticeResponse> update(
		@PathVariable Long id,
		@Valid @RequestBody NoticeRequest request
	) {
		return ApiResult.success(adminNoticeService.update(id, request));
	}

	@Override
	@ResponseStatus(OK)
	@DeleteMapping("/{id}")
	public ApiResult<Void> delete(@PathVariable Long id) {
		adminNoticeService.delete(id);
		return ApiResult.<Void>success(null);
	}
}
