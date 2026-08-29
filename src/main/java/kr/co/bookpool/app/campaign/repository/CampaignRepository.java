package kr.co.bookpool.app.campaign.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kr.co.bookpool.app.campaign.entity.Campaign;
import kr.co.bookpool.app.campaign.entity.CampaignCategory;
import kr.co.bookpool.app.campaign.entity.CampaignStatus;
import kr.co.bookpool.app.campaign.entity.PublishStatus;

public interface CampaignRepository
	extends JpaRepository<Campaign, Long>, JpaSpecificationExecutor<Campaign> {

	List<Campaign> findAllByIdIn(List<Long> ids);

	List<Campaign> findAllByIdInAndPublishStatus(List<Long> ids, PublishStatus publishStatus);

	Optional<Campaign> findByIdAndPublishStatus(Long id, PublishStatus publishStatus);

	@Modifying(clearAutomatically = true)
	@Query("update Campaign c set c.viewCount = c.viewCount + 1 where c.id = :id")
	int increaseViewCount(@Param("id") Long id);

	@Query("select distinct c.publisherName from Campaign c "
		+ "where c.publishStatus = :publishStatus order by c.publisherName")
	List<String> findDistinctPublishers(@Param("publishStatus") PublishStatus publishStatus);

	/**
	 * 홈 카테고리 타일의 '모집중 n건'.
	 * 목록을 전부 읽어와 세지 않도록 집계만 가져온다.
	 */
	@Query("select c.category, count(c) from Campaign c "
		+ "where c.publishStatus = :publishStatus and c.status = :status "
		+ "group by c.category")
	List<Object[]> countByCategory(
		@Param("publishStatus") PublishStatus publishStatus,
		@Param("status") CampaignStatus status
	);

	boolean existsByDedupeKey(String dedupeKey);

	Optional<Campaign> findFirstByDedupeKey(String dedupeKey);

	List<Campaign> findAllByCategory(CampaignCategory category);
}
