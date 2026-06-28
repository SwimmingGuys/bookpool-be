package kr.co.bookpool.app.notice.controller;

import static org.springframework.http.HttpStatus.*;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import kr.co.bookpool.common.response.PageResponse;
import kr.co.bookpool.app.notice.controller.docs.NoticeControllerDocs;
import kr.co.bookpool.app.notice.dto.response.NoticeResponse;
import kr.co.bookpool.app.notice.entity.NoticeCategory;
import kr.co.bookpool.app.notice.service.NoticeService;
import kr.co.bookpool.common.response.ApiResult;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices")
public class NoticeController implements NoticeControllerDocs {

	private final NoticeService noticeService;

	@Override
	@ResponseStatus(OK)
	@GetMapping
	public ApiResult<PageResponse<NoticeResponse>> list(
		@RequestParam(required = false) NoticeCategory category,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResult.success(noticeService.list(category, page, size));
	}

	@Override
	@ResponseStatus(OK)
	@GetMapping("/{id}")
	public ApiResult<NoticeResponse> detail(@PathVariable Long id) {
		return ApiResult.success(noticeService.detail(id));
	}
}
