package kr.co.bookpool.app.notification.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record NotificationSubscriptionDto(
	@NotNull List<String> types,
	@NotNull List<String> categories,
	@NotNull List<String> publishers
) {

	public static NotificationSubscriptionDto empty() {
		return new NotificationSubscriptionDto(List.of(), List.of(), List.of());
	}
}
