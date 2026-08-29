package kr.co.bookpool.app.inquiry.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import kr.co.bookpool.app.inquiry.entity.Inquiry;
import kr.co.bookpool.app.inquiry.entity.InquiryStatus;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

	Page<Inquiry> findAllByMemberId(Long memberId, Pageable pageable);

	Optional<Inquiry> findByIdAndMemberId(Long id, Long memberId);

	// 관리자 목록은 작성자를 함께 보여주므로 N+1을 피해 한 번에 가져온다.
	@Override
	@EntityGraph(attributePaths = "member")
	Page<Inquiry> findAll(Pageable pageable);

	@EntityGraph(attributePaths = "member")
	Page<Inquiry> findAllByStatus(InquiryStatus status, Pageable pageable);
}
