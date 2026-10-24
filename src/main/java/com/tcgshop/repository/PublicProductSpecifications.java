package com.tcgshop.repository;

import com.tcgshop.dto.request.PublicProductSearchRequest;
import com.tcgshop.entity.Product;
import com.tcgshop.entity.enums.ProductStatus;
import com.tcgshop.entity.enums.ProductType;
import com.tcgshop.entity.enums.ShopStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class PublicProductSpecifications {

	private PublicProductSpecifications() {
	}

	public static Specification<Product> from(PublicProductSearchRequest filters) {
		return (root, query, criteriaBuilder) -> {
			Join<Object, Object> shop = root.join("shop", JoinType.INNER);
			Join<Object, Object> card = root.join("card", JoinType.LEFT);
			Join<Object, Object> cardSeries = card.join("series", JoinType.LEFT);
			Join<Object, Object> cardGame = cardSeries.join("game", JoinType.LEFT);
			Join<Object, Object> productSeries = root.join("series", JoinType.LEFT);
			Join<Object, Object> productGame = productSeries.join("game", JoinType.LEFT);

			List<Predicate> predicates = new ArrayList<>();
			predicates.add(criteriaBuilder.equal(root.get("status"), ProductStatus.ACTIVE));
			predicates.add(criteriaBuilder.isNotNull(root.get("price")));
			predicates.add(criteriaBuilder.greaterThan(root.get("price"), BigDecimal.ZERO));
			predicates.add(criteriaBuilder.or(
					criteriaBuilder.isFalse(root.get("deleted")),
					criteriaBuilder.isNull(root.get("deleted"))));
			predicates.add(criteriaBuilder.equal(shop.get("status"), ShopStatus.APPROVED));
			predicates.add(criteriaBuilder.or(
					criteriaBuilder.isFalse(shop.get("deleted")),
					criteriaBuilder.isNull(shop.get("deleted"))));
			predicates.add(criteriaBuilder.or(
					criteriaBuilder.notEqual(root.get("type"), ProductType.SINGLE),
					criteriaBuilder.and(
							criteriaBuilder.isNotNull(card.get("id")),
							criteriaBuilder.isTrue(card.get("active")),
							criteriaBuilder.isTrue(cardSeries.get("active")),
							criteriaBuilder.isTrue(cardGame.get("active")))));
			predicates.add(criteriaBuilder.or(
					criteriaBuilder.isNull(productSeries.get("id")),
					criteriaBuilder.and(
							criteriaBuilder.isTrue(productSeries.get("active")),
							criteriaBuilder.isTrue(productGame.get("active")))));

			if (filters.getKeyword() != null && !filters.getKeyword().isBlank()) {
				String keyword = "%" + filters.getKeyword().trim().toLowerCase() + "%";
				predicates.add(criteriaBuilder.or(
						criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), keyword),
						criteriaBuilder.like(criteriaBuilder.lower(card.get("name")), keyword),
						criteriaBuilder.like(criteriaBuilder.lower(card.get("cardNumber")), keyword)));
			}
			if (filters.getGameId() != null) {
				predicates.add(criteriaBuilder.or(
						criteriaBuilder.equal(cardGame.get("id"), filters.getGameId()),
						criteriaBuilder.equal(productGame.get("id"), filters.getGameId())));
			}
			if (filters.getSeriesId() != null) {
				predicates.add(criteriaBuilder.or(
						criteriaBuilder.equal(cardSeries.get("id"), filters.getSeriesId()),
						criteriaBuilder.equal(productSeries.get("id"), filters.getSeriesId())));
			}
			if (filters.getRarity() != null) {
				predicates.add(criteriaBuilder.equal(card.get("rarity"), filters.getRarity()));
			}
			if (filters.getType() != null) {
				predicates.add(criteriaBuilder.equal(root.get("type"), filters.getType()));
			}
			if (filters.getLanguage() != null) {
				predicates.add(criteriaBuilder.equal(
						criteriaBuilder.coalesce(root.get("language"), card.get("language")),
						filters.getLanguage()));
			}
			if (filters.getCondition() != null) {
				predicates.add(criteriaBuilder.equal(root.get("cardCondition"), filters.getCondition()));
			}
			if (filters.getPriceFrom() != null) {
				predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), filters.getPriceFrom()));
			}
			if (filters.getPriceTo() != null) {
				predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), filters.getPriceTo()));
			}

			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}
}
