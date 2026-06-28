package kr.co.bookpool.app.campaign.controller;

import static org.springframework.http.HttpStatus.*;

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
import kr.co.bookpool.app.campaign.controller.docs.AdminCampaignControllerDocs;
import kr.co.bookpool.app.campaign.dto.request.CampaignCreateRequest;
import kr.co.bookpool.app.campaign.dto.request.CampaignUpdateRequest;
import kr.co.bookpool.app.campaign.dto.response.CampaignResponse;
import kr.co.bookpool.app.campaign.dto.response.PageResponse;
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
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResult.success(adminCampaignService.list(page, size));
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
	@DeleteMapping("/{id}")
	public ApiResult<Void> delete(@PathVariable Long id) {
		adminCampaignService.delete(id);
		return ApiResult.<Void>success(null);
	}
}
