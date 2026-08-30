package kr.co.bookpool.app.application.dto.request;

import jakarta.validation.constraints.NotNull;
import kr.co.bookpool.app.application.entity.ApplicationStatus;

public record ApplicationStatusRequest(
	@NotNull ApplicationStatus status
) {
}
