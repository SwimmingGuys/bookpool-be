package kr.co.bookpool.app.upload.controller;

import static org.springframework.http.HttpStatus.*;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import kr.co.bookpool.app.upload.controller.docs.AdminUploadControllerDocs;
import kr.co.bookpool.app.upload.dto.UploadResponse;
import kr.co.bookpool.app.upload.storage.ImageStorage;
import kr.co.bookpool.common.response.ApiResult;
import lombok.RequiredArgsConstructor;

/**
 * 표지 이미지 업로드.
 * 지금은 관리자만 쓰므로 /api/admin 아래에 둬 기존 권한 규칙을 그대로 따른다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/uploads")
public class AdminUploadController implements AdminUploadControllerDocs {

	private final ImageStorage imageStorage;

	@Override
	@ResponseStatus(CREATED)
	@PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResult<UploadResponse> uploadImage(@RequestPart("file") MultipartFile file) {
		return ApiResult.success(new UploadResponse(imageStorage.store(file)));
	}
}
