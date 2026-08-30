package kr.co.bookpool.app.application.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kr.co.bookpool.app.application.entity.Application;
import kr.co.bookpool.app.application.entity.ApplicationStatus;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

	@Query("select a.campaign.id from Application a where a.member.id = :memberId")
	List<Long> findCampaignIdsByMemberId(@Param("memberId") Long memberId);

	// 목록에 공고 정보를 함께 보여주므로 N+1을 피해 한 번에 가져온다.
	@Query(
		value = "select a from Application a join fetch a.campaign where a.member.id = :memberId",
		countQuery = "select count(a) from Application a where a.member.id = :memberId"
	)
	Page<Application> findAllByMemberIdWithCampaign(@Param("memberId") Long memberId, Pageable pageable);

	@Query(
		value = "select a from Application a join fetch a.campaign "
			+ "where a.member.id = :memberId and a.status = :status",
		countQuery = "select count(a) from Application a where a.member.id = :memberId and a.status = :status"
	)
	Page<Application> findAllByMemberIdAndStatusWithCampaign(
		@Param("memberId") Long memberId,
		@Param("status") ApplicationStatus status,
		Pageable pageable
	);

	Optional<Application> findByMemberIdAndCampaignId(Long memberId, Long campaignId);

	long deleteByMemberIdAndCampaignId(Long memberId, Long campaignId);

	boolean existsByMemberIdAndCampaignId(Long memberId, Long campaignId);
}
