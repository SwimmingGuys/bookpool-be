package kr.co.bookpool.app.inquiry.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AnswerInquiryRequest(
	@NotBlank String answer
) {
}
