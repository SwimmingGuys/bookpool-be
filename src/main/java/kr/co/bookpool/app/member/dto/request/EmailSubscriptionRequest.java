package kr.co.bookpool.app.member.dto.request;

import jakarta.validation.constraints.NotNull;

public record EmailSubscriptionRequest(
	@NotNull Boolean emailSubscribed
) {
}
