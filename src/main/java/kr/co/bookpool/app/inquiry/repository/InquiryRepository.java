package kr.co.bookpool.app.inquiry.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import kr.co.bookpool.app.inquiry.entity.Inquiry;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

	Page<Inquiry> findAllByMemberId(Long memberId, Pageable pageable);

	Optional<Inquiry> findByIdAndMemberId(Long id, Long memberId);
}
