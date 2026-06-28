package kr.co.bookpool.app.campaign.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kr.co.bookpool.app.campaign.entity.Campaign;

public interface CampaignRepository
	extends JpaRepository<Campaign, Long>, JpaSpecificationExecutor<Campaign> {

	List<Campaign> findAllByIdIn(List<Long> ids);

	@Modifying(clearAutomatically = true)
	@Query("update Campaign c set c.viewCount = c.viewCount + 1 where c.id = :id")
	int increaseViewCount(@Param("id") Long id);
}
