package kr.co.bookpool.app.notice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.co.bookpool.app.notice.entity.NoticeCategory;

public record NoticeRequest(
	@NotBlank String title,
	@NotBlank String content,
	@NotNull NoticeCategory category,
	// 프론트는 isPinned로 보낸다. Jackson이 record 컴포넌트명과 맞추도록 이름을 유지한다.
	boolean isPinned
) {
}
