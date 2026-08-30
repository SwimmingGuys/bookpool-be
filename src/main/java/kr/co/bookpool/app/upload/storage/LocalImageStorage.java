package kr.co.bookpool.app.upload.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import kr.co.bookpool.common.exception.BusinessException;
import kr.co.bookpool.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 로컬 디스크 저장 구현(기본값).
 *
 * <p>컨테이너에 배포하면 재시작 시 파일이 사라지므로, 운영에서는 오브젝트 스토리지
 * 구현으로 교체해야 한다. 볼륨을 붙여 쓰는 것도 가능하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalImageStorage implements ImageStorage {

	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

	private final UploadProperties properties;

	@Override
	public String store(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}
		if (file.getSize() > properties.maxSizeBytes()) {
			throw new BusinessException(ErrorCode.UPLOAD_TOO_LARGE);
		}

		String contentType = file.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			throw new BusinessException(ErrorCode.UPLOAD_UNSUPPORTED_TYPE);
		}

		String extension = extensionOf(file.getOriginalFilename());
		if (!ALLOWED_EXTENSIONS.contains(extension)) {
			throw new BusinessException(ErrorCode.UPLOAD_UNSUPPORTED_TYPE);
		}

		// 원본 파일명은 그대로 쓰지 않는다(경로 조작·중복 방지).
		String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
		// 한 디렉터리에 파일이 무한정 쌓이지 않도록 날짜로 나눈다.
		String datePath = LocalDate.now().toString();

		try {
			Path directory = Path.of(properties.directory(), datePath).toAbsolutePath().normalize();
			Files.createDirectories(directory);
			Path target = directory.resolve(storedName);
			try (var input = file.getInputStream()) {
				Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
			}
			log.info("[업로드] {} 저장", target);
		} catch (IOException e) {
			log.error("[업로드] 저장 실패", e);
			throw new BusinessException(ErrorCode.UPLOAD_FAILED);
		}

		return properties.publicPath() + "/" + datePath + "/" + storedName;
	}

	private String extensionOf(String originalFilename) {
		if (originalFilename == null) return "";
		int dot = originalFilename.lastIndexOf('.');
		if (dot < 0 || dot == originalFilename.length() - 1) return "";
		return originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
	}
}
