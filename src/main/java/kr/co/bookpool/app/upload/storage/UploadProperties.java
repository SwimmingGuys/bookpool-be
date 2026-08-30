package kr.co.bookpool.app.upload.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param directory 파일을 쓸 디렉터리 (로컬 저장소 전용)
 * @param publicPath 저장된 파일을 서빙할 URL 경로 접두사
 * @param maxSizeBytes 허용 최대 용량
 */
@ConfigurationProperties(prefix = "app.upload")
public record UploadProperties(
	String directory,
	String publicPath,
	Long maxSizeBytes
) {

	public UploadProperties {
		if (directory == null || directory.isBlank()) directory = "uploads";
		if (publicPath == null || publicPath.isBlank()) publicPath = "/uploads";
		if (maxSizeBytes == null || maxSizeBytes <= 0) maxSizeBytes = 5L * 1024 * 1024;
	}
}
