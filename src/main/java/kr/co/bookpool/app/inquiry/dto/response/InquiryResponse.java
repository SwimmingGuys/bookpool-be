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
	LocalDateTime answeredAt,
	// 관리자 목록에서만 채워진다. 사용자 화면에서는 null.
	String authorEmail,
	String authorNickname
) {

	public static InquiryResponse from(Inquiry inquiry) {
		return of(inquiry, false);
	}

	/** 관리자 화면용. 누가 남긴 문의인지 함께 내려준다. */
	public static InquiryResponse forAdmin(Inquiry inquiry) {
		return of(inquiry, true);
	}

	private static InquiryResponse of(Inquiry inquiry, boolean includeAuthor) {
		return new InquiryResponse(
			String.valueOf(inquiry.getId()),
			inquiry.getType().name().toLowerCase(Locale.ROOT),
			inquiry.getTitle(),
			inquiry.getContent(),
			inquiry.getStatus().name().toLowerCase(Locale.ROOT),
			inquiry.getCreatedAt(),
			inquiry.getAnswer(),
			inquiry.getAnsweredAt(),
			includeAuthor ? inquiry.getMember().getEmail() : null,
			includeAuthor ? inquiry.getMember().getNickname() : null
		);
	}
}
