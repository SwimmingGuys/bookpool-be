package kr.co.bookpool.app.campaign.controller;

import static org.springframework.http.HttpStatus.*;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import kr.co.bookpool.app.campaign.controller.docs.CampaignControllerDocs;
import kr.co.bookpool.app.campaign.dto.request.CampaignSearchCondition;
import kr.co.bookpool.app.campaign.dto.request.DeadlineFilter;
import kr.co.bookpool.app.campaign.dto.request.SortKey;
import kr.co.bookpool.app.campaign.dto.response.CampaignResponse;
import kr.co.bookpool.app.campaign.dto.response.PageResponse;
import kr.co.bookpool.app.campaign.entity.CampaignCategory;
import kr.co.bookpool.app.campaign.entity.CampaignType;
import kr.co.bookpool.app.campaign.service.CampaignService;
import kr.co.bookpool.common.response.ApiResult;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/campaigns")
public class CampaignController implements CampaignControllerDocs {

	private final CampaignService campaignService;

	@Override
	@ResponseStatus(OK)
	@GetMapping
	public ApiResult<PageResponse<CampaignResponse>> list(
		@RequestParam(required = false) String query,
		@RequestParam(required = false) List<CampaignCategory> categories,
		@RequestParam(required = false) List<CampaignType> types,
		@RequestParam(required = false, defaultValue = "ALL") DeadlineFilter deadline,
		@RequestParam(required = false, defaultValue = "DEADLINE") SortKey sort,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		CampaignSearchCondition condition = CampaignSearchCondition.builder()
			.query(query)
			.categories(categories)
			.types(types)
			.deadline(deadline)
			.sort(sort)
			.build();
		return ApiResult.success(campaignService.search(condition, page, size));
	}

	@Override
	@ResponseStatus(OK)
	@GetMapping("/{id}")
	public ApiResult<CampaignResponse> detail(@PathVariable Long id) {
		return ApiResult.success(campaignService.getAndIncreaseView(id));
	}

	@Override
	@ResponseStatus(OK)
	@GetMapping("/publishers")
	public ApiResult<List<String>> publishers() {
		return ApiResult.success(campaignService.getAllPublishers());
	}
}
