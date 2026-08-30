package kr.co.bookpool.app.upload.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 저장 추상화.
 * 로컬 디스크 구현({@link LocalImageStorage})이 기본이고, 운영에서는 오브젝트 스토리지
 * 구현으로 교체한다. (컨테이너의 로컬 디스크는 재배포하면 사라진다)
 */
public interface ImageStorage {

	/** 파일을 저장하고 공개적으로 접근 가능한 URL을 돌려준다. */
	String store(MultipartFile file);
}
