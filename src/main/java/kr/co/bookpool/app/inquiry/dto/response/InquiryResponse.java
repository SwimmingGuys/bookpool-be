package kr.co.bookpool.app.inquiry.dto.response;

import java.time.LocalDateTime;
import java.util.Locale;

import kr.co.bookpool.app.inquiry.entity.Inquiry;

public record InquiryResponse(
	String id,
	String type,
	String title,
	String content,
	String status,
	LocalDateTime createdAt,
	String answer,
	LocalDateTime answeredAt
) {

	public static InquiryResponse from(Inquiry inquiry) {
		return new InquiryResponse(
			String.valueOf(inquiry.getId()),
			inquiry.getType().name().toLowerCase(Locale.ROOT),
			inquiry.getTitle(),
			inquiry.getContent(),
			inquiry.getStatus().name().toLowerCase(Locale.ROOT),
			inquiry.getCreatedAt(),
			inquiry.getAnswer(),
			inquiry.getAnsweredAt()
		);
	}
}
