package kr.co.bookpool.common.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import kr.co.bookpool.app.upload.storage.UploadProperties;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(UploadProperties.class)
public class WebMvcConfig implements WebMvcConfigurer {

	private final UploadProperties uploadProperties;

	@Override
	public void addFormatters(@NonNull FormatterRegistry registry) {
		// 쿼리 파라미터 enum도 요청 본문과 같은 규칙(한국어 라벨·별칭)으로 해석한다.
		registry.addConverterFactory(new StringToEnumConverterFactory());
	}

	@Override
	public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
		// 로컬 저장소에 쓴 이미지를 그대로 서빙한다.
		// 오브젝트 스토리지로 바꾸면 이 핸들러는 필요 없다.
		Path directory = Path.of(uploadProperties.directory()).toAbsolutePath().normalize();
		registry.addResourceHandler(uploadProperties.publicPath() + "/**")
			.addResourceLocations(directory.toUri().toString());
	}
}
