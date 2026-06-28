package kr.co.bookpool.app.campaign.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import kr.co.bookpool.app.campaign.dto.request.CampaignSearchCondition;
import kr.co.bookpool.app.campaign.dto.request.DeadlineFilter;
import kr.co.bookpool.app.campaign.entity.Campaign;

public final class CampaignSpecifications {

	private CampaignSpecifications() {
	}

	public static Specification<Campaign> of(CampaignSearchCondition cond) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (cond.query() != null && !cond.query().isBlank()) {
				String like = "%" + cond.query().trim().toLowerCase() + "%";
				predicates.add(cb.or(
					cb.like(cb.lower(root.get("title")), like),
					cb.like(cb.lower(root.get("bookTitle")), like),
					cb.like(cb.lower(root.get("publisherName")), like)
				));
			}

			if (cond.categories() != null && !cond.categories().isEmpty()) {
				predicates.add(root.get("category").in(cond.categories()));
			}

			if (cond.types() != null && !cond.types().isEmpty()) {
				predicates.add(root.get("type").in(cond.types()));
			}

			DeadlineFilter deadline = cond.deadline();
			if (deadline != null && deadline != DeadlineFilter.ALL) {
				LocalDateTime now = LocalDateTime.now();
				long days = deadline == DeadlineFilter.WEEK ? 7 : 3;
				predicates.add(cb.between(root.get("deadlineAt"), now, now.plusDays(days)));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}
