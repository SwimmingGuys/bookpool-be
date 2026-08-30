package kr.co.bookpool.app.upload.controller.docs;

import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.bookpool.app.upload.dto.UploadResponse;
import kr.co.bookpool.common.response.ApiResult;

@Tag(name = "Admin Upload", description = "백오피스 파일 업로드 API (ROLE_ADMIN 전용)")
public interface AdminUploadControllerDocs {

	@Operation(
		summary = "이미지 업로드 (관리자)",
		description = "표지 이미지를 저장하고 접근 URL을 반환합니다. multipart/form-data의 file 파트로 보냅니다.",
		security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "업로드 성공"),
		@ApiResponse(responseCode = "400", description = "이미지가 아니거나 용량 초과 (U001/U002)"),
		@ApiResponse(responseCode = "403", description = "관리자 권한 없음")
	})
	ApiResult<UploadResponse> uploadImage(MultipartFile file);
}
