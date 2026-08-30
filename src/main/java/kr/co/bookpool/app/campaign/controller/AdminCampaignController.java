package kr.co.bookpool.app.campaign.controller;

import static org.springframework.http.HttpStatus.*;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.co.bookpool.app.campaign.controller.docs.AdminCampaignControllerDocs;
import kr.co.bookpool.app.campaign.dto.request.CampaignCreateRequest;
import kr.co.bookpool.app.campaign.dto.request.CampaignUpdateRequest;
import kr.co.bookpool.app.campaign.dto.request.PublishStatusUpdateRequest;
import kr.co.bookpool.app.campaign.dto.request.StatusUpdateRequest;
import kr.co.bookpool.app.campaign.dto.response.CampaignResponse;
import kr.co.bookpool.app.campaign.entity.PublishStatus;
import kr.co.bookpool.common.response.PageResponse;
import kr.co.bookpool.app.campaign.service.AdminCampaignService;
import kr.co.bookpool.common.response.ApiResult;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/campaigns")
public class AdminCampaignController implements AdminCampaignControllerDocs {

	private final AdminCampaignService adminCampaignService;

	@Override
	@ResponseStatus(OK)
	@GetMapping
	public ApiResult<PageResponse<CampaignResponse>> list(
		@RequestParam(required = false) PublishStatus publishStatus,
		@RequestParam(required = false) String query,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResult.success(adminCampaignService.list(publishStatus, query, page, size));
	}

	@Override
	@ResponseStatus(OK)
	@GetMapping("/{id}")
	public ApiResult<CampaignResponse> detail(@PathVariable Long id) {
		return ApiResult.success(adminCampaignService.get(id));
	}

	@Override
	@ResponseStatus(CREATED)
	@PostMapping
	public ApiResult<CampaignResponse> create(@Valid @RequestBody CampaignCreateRequest request) {
		return ApiResult.success(adminCampaignService.create(request));
	}

	@Override
	@ResponseStatus(OK)
	@PutMapping("/{id}")
	public ApiResult<CampaignResponse> update(
		@PathVariable Long id,
		@Valid @RequestBody CampaignUpdateRequest request
	) {
		return ApiResult.success(adminCampaignService.update(id, request));
	}

	@Override
	@ResponseStatus(OK)
	@PatchMapping("/{id}/publish-status")
	public ApiResult<CampaignResponse> changePublishStatus(
		@PathVariable Long id,
		@Valid @RequestBody PublishStatusUpdateRequest request
	) {
		return ApiResult.success(adminCampaignService.changePublishStatus(id, request.publishStatus()));
	}

	@Override
	@ResponseStatus(OK)
	@PatchMapping("/{id}/status")
	public ApiResult<CampaignResponse> changeStatus(
		@PathVariable Long id,
		@Valid @RequestBody StatusUpdateRequest request
	) {
		return ApiResult.success(adminCampaignService.changeStatus(id, request.status()));
	}

	@Override
	@ResponseStatus(OK)
	@DeleteMapping("/{id}")
	public ApiResult<Void> delete(@PathVariable Long id) {
		adminCampaignService.delete(id);
		return ApiResult.<Void>success(null);
	}
}
