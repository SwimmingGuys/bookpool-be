package kr.co.bookpool.common.exception;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DbConstraintTest {

	@Test
	@DisplayName("이메일 unique 제약 위반 메시지는 DUPLICATE_EMAIL로 매핑된다")
	void resolve_memberEmailConstraint() {
		// given - H2가 명명된 unique 제약 위반 시 내보내는 형태(인덱스 이름에 제약명을 대문자로 포함)
		String message = "Unique index or primary key violation: "
			+ "\"PUBLIC.UK_MEMBER_EMAIL_INDEX_6 ON PUBLIC.MEMBER(EMAIL NULLS FIRST) VALUES ( /* key:1 */ )\"";

		// when
		Optional<ErrorCode> resolved = DbConstraint.resolve(message);

		// then
		assertThat(resolved).contains(ErrorCode.DUPLICATE_EMAIL);
	}

	@Test
	@DisplayName("알 수 없는 제약 위반 메시지는 매핑되지 않는다")
	void resolve_unknownConstraint() {
		// given
		String message = "Unique index or primary key violation: \"UK_SOMETHING_ELSE ...\"";

		// when
		Optional<ErrorCode> resolved = DbConstraint.resolve(message);

		// then
		assertThat(resolved).isEmpty();
	}

	@Test
	@DisplayName("원인 메시지가 null이면 매핑되지 않는다")
	void resolve_nullMessage() {
		assertThat(DbConstraint.resolve(null)).isEmpty();
	}
}
