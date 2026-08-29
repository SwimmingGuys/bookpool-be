package kr.co.bookpool.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

	@Override
	public void addFormatters(@NonNull FormatterRegistry registry) {
		// 쿼리 파라미터 enum도 요청 본문과 같은 규칙(한국어 라벨·별칭)으로 해석한다.
		registry.addConverterFactory(new StringToEnumConverterFactory());
	}
}
