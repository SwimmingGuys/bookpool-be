package kr.co.bookpool.app.notification.dto.response;

import java.time.LocalDateTime;

import kr.co.bookpool.app.campaign.entity.Campaign;
import kr.co.bookpool.app.notification.entity.Notification;

public record NotificationResponse(
	String id,
	String kind,
	String campaignId,
	String campaignTitle,
	String bookTitle,
	String publisherName,
	String message,
	boolean isRead,
	LocalDateTime createdAt
) {

	public static NotificationResponse from(Notification notification) {
		Campaign campaign = notification.getCampaign();
		return new NotificationResponse(
			String.valueOf(notification.getId()),
			notification.getKind().name(),
			campaign == null ? null : String.valueOf(campaign.getId()),
			campaign == null ? null : campaign.getTitle(),
			campaign == null ? null : campaign.getBookTitle(),
			campaign == null ? null : campaign.getPublisherName(),
			notification.getMessage(),
			notification.isRead(),
			notification.getCreatedAt()
		);
	}
}
