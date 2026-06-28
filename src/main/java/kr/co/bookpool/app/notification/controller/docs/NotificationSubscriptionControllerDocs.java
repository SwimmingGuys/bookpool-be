package kr.co.bookpool.app.notification.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.bookpool.app.notification.dto.NotificationSubscriptionDto;
import kr.co.bookpool.common.response.ApiResult;

@Tag(name = "Notification Subscription", description = "알림 구독 설정 API")
public interface NotificationSubscriptionControllerDocs {

	@Operation(
		summary = "내 알림 구독 설정 조회",
		description = "관심 유형/카테고리/출판사 구독 설정을 조회합니다. 설정한 적이 없으면 빈 목록을 반환합니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponse(
		responseCode = "200", description = "조회 성공",
		content = @Content(examples = @ExampleObject(value = """
			{
			  "success": true,
			  "code": "SUCCESS",
			  "message": "요청에 성공했습니다.",
			  "data": { "types": ["Reviewer"], "categories": ["IT/개발"], "publishers": ["인사이트"] }
			}""")))
	ApiResult<NotificationSubscriptionDto> get(Long memberId);

	@Operation(
		summary = "내 알림 구독 설정 저장",
		description = "구독 설정을 통째로 저장(업서트)합니다. 각 목록은 비어 있을 수 있습니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponse(responseCode = "200", description = "저장 성공")
	ApiResult<NotificationSubscriptionDto> save(Long memberId, NotificationSubscriptionDto request);
}
