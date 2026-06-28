package kr.co.bookpool.app.notice.dto.response;

import java.time.LocalDateTime;
import java.util.Locale;

import kr.co.bookpool.app.notice.entity.Notice;

public record NoticeResponse(
	String id,
	String title,
	String content,
	String category,
	String author,
	boolean isPinned,
	LocalDateTime createdAt
) {

	public static NoticeResponse from(Notice notice) {
		return new NoticeResponse(
			String.valueOf(notice.getId()),
			notice.getTitle(),
			notice.getContent(),
			notice.getCategory().name().toLowerCase(Locale.ROOT),
			notice.getAdmin().getNickname(),
			notice.isPinned(),
			notice.getCreatedAt()
		);
	}
}
