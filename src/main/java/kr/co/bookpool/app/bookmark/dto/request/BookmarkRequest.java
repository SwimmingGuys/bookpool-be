package kr.co.bookpool.app.bookmark.dto.request;

import jakarta.validation.constraints.NotNull;

public record BookmarkRequest(@NotNull Long campaignId) {
}
