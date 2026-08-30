package kr.co.bookpool.app.application.dto.request;

import jakarta.validation.constraints.NotNull;
import kr.co.bookpool.app.application.entity.ApplicationStatus;

public record ApplicationRequest(
	@NotNull Long campaignId,
	// 생략하면 APPLIED(신청함)로 시작한다.
	ApplicationStatus status
) {
}
