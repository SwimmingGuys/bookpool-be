package kr.co.bookpool.app.recentview.dto.request;

import jakarta.validation.constraints.NotNull;

public record RecentViewRequest(@NotNull Long campaignId) {
}
