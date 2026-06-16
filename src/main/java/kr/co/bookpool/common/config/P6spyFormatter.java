package kr.co.bookpool.common.config;

import java.util.Locale;

import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.springframework.context.annotation.Configuration;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.P6SpyOptions;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;

import jakarta.annotation.PostConstruct;

/**
 * p6spy가 출력하는 쿼리 로그를 Hibernate 포맷으로 정리해 가독성을 높인다.
 * 파라미터가 바인딩된 실제 실행 쿼리 + 실행 시간을 함께 보여준다.
 */
@Configuration
public class P6spyFormatter implements MessageFormattingStrategy {

	@PostConstruct
	public void register() {
		P6SpyOptions.getActiveInstance().setLogMessageFormat(this.getClass().getName());
	}

	@Override
	public String formatMessage(int connectionId, String now, long elapsed, String category,
		String prepared, String sql, String url) {

		if (sql == null || sql.isBlank()) {
			return "";
		}
		return "[%s] | %d ms%s".formatted(category, elapsed, format(category, sql));
	}

	private String format(String category, String sql) {
		if (!Category.STATEMENT.getName().equals(category)) {
			return "\n" + sql;
		}

		String trimmed = sql.trim().toLowerCase(Locale.ROOT);
		FormatStyle style = (trimmed.startsWith("create") || trimmed.startsWith("alter")
			|| trimmed.startsWith("drop") || trimmed.startsWith("comment"))
			? FormatStyle.DDL
			: FormatStyle.BASIC;

		return style.getFormatter().format(sql);
	}
}
