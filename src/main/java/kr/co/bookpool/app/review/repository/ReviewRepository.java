package kr.co.bookpool.app.review.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import kr.co.bookpool.app.review.entity.Review;
import kr.co.bookpool.app.review.entity.ReviewStatus;
import kr.co.bookpool.app.review.entity.ReviewSubmissionStatus;

public interface ReviewRepository extends JpaRepository<Review, Long> {

	// 서평은 공고·작성자와 함께 보여주므로 항상 같이 가져온다.
	String GRAPH = "campaign";

	@EntityGraph(attributePaths = {"campaign", "member"})
	Page<Review> findAllByCampaignIdAndStatus(Long campaignId, ReviewStatus status, Pageable pageable);

	@EntityGraph(attributePaths = {"campaign", "member"})
	Page<Review> findAllByMemberId(Long memberId, Pageable pageable);

	@EntityGraph(attributePaths = {"campaign", "member"})
	Page<Review> findAllByCampaignId(Long campaignId, Pageable pageable);

	@EntityGraph(attributePaths = {"campaign", "member"})
	Page<Review> findAllBySubmissionStatus(ReviewSubmissionStatus submissionStatus, Pageable pageable);

	@EntityGraph(attributePaths = {"campaign", "member"})
	Page<Review> findAllByCampaignIdAndSubmissionStatus(
		Long campaignId, ReviewSubmissionStatus submissionStatus, Pageable pageable);

	@Override
	@EntityGraph(attributePaths = {"campaign", "member"})
	Page<Review> findAll(Pageable pageable);

	@EntityGraph(attributePaths = {"campaign", "member"})
	Optional<Review> findWithCampaignById(Long id);

	boolean existsByCampaignIdAndMemberId(Long campaignId, Long memberId);
}
