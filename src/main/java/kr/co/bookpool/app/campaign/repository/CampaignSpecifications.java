package kr.co.bookpool.app.campaign.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import kr.co.bookpool.app.campaign.dto.request.CampaignSearchCondition;
import kr.co.bookpool.app.campaign.dto.request.DateBasis;
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

			// 출판사 페이지는 정확히 일치하는 출판사만 본다.
			if (cond.publisher() != null && !cond.publisher().isBlank()) {
				predicates.add(cb.equal(
					cb.lower(root.get("publisherName")),
					cond.publisher().trim().toLowerCase()
				));
			}

			if (cond.categories() != null && !cond.categories().isEmpty()) {
				predicates.add(root.get("category").in(cond.categories()));
			}

			if (cond.types() != null && !cond.types().isEmpty()) {
				predicates.add(root.get("type").in(cond.types()));
			}

			if (cond.publishStatus() != null) {
				predicates.add(cb.equal(root.get("publishStatus"), cond.publishStatus()));
			}

			Integer withinDays = cond.effectiveWithinDays();
			if (withinDays != null) {
				LocalDateTime now = LocalDateTime.now();
				predicates.add(cb.between(root.get("deadlineAt"), now, now.plusDays(withinDays)));
			}

			addDateRange(cond, root, cb, predicates);

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	/**
	 * 캘린더가 보고 있는 달만 받아가기 위한 범위 조건.
	 * deadlineAt은 시각까지 가진 값이라 그 날의 끝까지 포함하도록 경계를 넓힌다.
	 */
	private static void addDateRange(
		CampaignSearchCondition cond,
		jakarta.persistence.criteria.Root<Campaign> root,
		jakarta.persistence.criteria.CriteriaBuilder cb,
		List<Predicate> predicates
	) {
		LocalDate from = cond.from();
		LocalDate to = cond.to();
		if (from == null && to == null) return;

		DateBasis basis = cond.dateBasis();
		String field = basis.getField();

		if (basis == DateBasis.RECRUIT_END) {
			if (from != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get(field), from.atStartOfDay()));
			}
			if (to != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get(field), to.atTime(LocalTime.MAX)));
			}
			return;
		}

		if (from != null) {
			predicates.add(cb.greaterThanOrEqualTo(root.get(field), from));
		}
		if (to != null) {
			predicates.add(cb.lessThanOrEqualTo(root.get(field), to));
		}
	}
}
