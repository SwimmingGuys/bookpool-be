package kr.co.bookpool.common.exception;

import static kr.co.bookpool.common.exception.ErrorCode.*;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * DB unique/무결성 제약 위반과 그에 대응하는 {@link ErrorCode} 매핑.
 *
 * 새로운 제약을 추가할 때는 엔티티에 명명한 제약 이름과 매핑할 ErrorCode를 상수로 한 줄 추가하면 된다.
 */
@Getter
@RequiredArgsConstructor
public enum DbConstraint {

	UK_MEMBER_EMAIL("uk_member_email", DUPLICATE_EMAIL);

	private final String constraintName;
	private final ErrorCode errorCode;

	/**
	 * 예외의 가장 구체적인 원인 메시지에서 알려진 제약 이름을 찾아 ErrorCode로 변환한다.
	 *
	 * @param causeMessage 예외 원인 메시지 (null 허용)
	 * @return 매칭되는 ErrorCode, 알 수 없는 제약이면 {@link Optional#empty()}
	 */
	public static Optional<ErrorCode> resolve(String causeMessage) {
		if (causeMessage == null) {
			return Optional.empty();
		}
		String lowerCased = causeMessage.toLowerCase(Locale.ROOT);
		return Arrays.stream(values())
			.filter(constraint -> lowerCased.contains(constraint.constraintName))
			.map(DbConstraint::getErrorCode)
			.findFirst();
	}
}