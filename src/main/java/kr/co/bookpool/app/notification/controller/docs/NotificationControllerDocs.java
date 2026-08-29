package kr.co.bookpool.app.notification.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.bookpool.app.notification.dto.response.NotificationResponse;
import kr.co.bookpool.common.response.ApiResult;
import kr.co.bookpool.common.response.PageResponse;

@Tag(name = "Notification", description = "알림 큐 API")
public interface NotificationControllerDocs {

	@Operation(
		summary = "내 알림 목록",
		description = "최신순으로 조회합니다. 구독 조건에 맞는 공고가 게시되면 여기에 쌓입니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponse(responseCode = "200", description = "조회 성공")
	ApiResult<PageResponse<NotificationResponse>> listMine(
		Long memberId,
		@Parameter(description = "페이지 번호 (0부터 시작)") int page,
		@Parameter(description = "페이지 크기") int size
	);

	@Operation(
		summary = "알림 읽음 처리",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "처리 성공"),
		@ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음 (NT001)")
	})
	ApiResult<Void> markRead(Long memberId, Long id);

	@Operation(
		summary = "알림 모두 읽음 처리",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponse(responseCode = "200", description = "처리 성공")
	ApiResult<Void> markAllRead(Long memberId);
}
