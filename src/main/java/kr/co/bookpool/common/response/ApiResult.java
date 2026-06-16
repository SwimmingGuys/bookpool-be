package kr.co.bookpool.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import kr.co.bookpool.common.exception.ErrorCode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResult<T>(
	boolean success,
	String code,
	String message,
	T data
) {

	private static final String SUCCESS_CODE = "SUCCESS";
	private static final String SUCCESS_MESSAGE = "요청에 성공했습니다.";

	public static <T> ApiResult<T> success(T data) {
		return new ApiResult<>(true, SUCCESS_CODE, SUCCESS_MESSAGE, data);
	}

	public static <T> ApiResult<T> success(String message, T data) {
		return new ApiResult<>(true, SUCCESS_CODE, message, data);
	}

	public static ApiResult<Void> error(ErrorCode errorCode) {
		return new ApiResult<>(false, errorCode.getCode(), errorCode.getMessage(), null);
	}

	public static <T> ApiResult<T> error(ErrorCode errorCode, T data) {
		return new ApiResult<>(false, errorCode.getCode(), errorCode.getMessage(), data);
	}
}