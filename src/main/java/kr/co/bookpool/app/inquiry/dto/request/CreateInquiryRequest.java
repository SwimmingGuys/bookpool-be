package kr.co.bookpool.app.inquiry.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.co.bookpool.app.inquiry.entity.InquiryType;

public record CreateInquiryRequest(
	@NotNull InquiryType type,
	@NotBlank @Size(max = 255) String title,
	@NotBlank @Size(max = 5000) String content
) {
}
