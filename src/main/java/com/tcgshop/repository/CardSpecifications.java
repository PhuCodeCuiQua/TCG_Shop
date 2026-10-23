package com.tcgshop.repository;

import com.tcgshop.entity.Card;
import com.tcgshop.entity.enums.Rarity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Điều kiện tìm kiếm động cho thẻ (PA-11). Mọi tham số lọc đều có thể null. */
public final class CardSpecifications {

	private CardSpecifications() {
	}

	/** Dùng cho Vendor chọn thẻ khi đăng bán: chỉ thẻ đang hiện thuộc series đang hiện. */
	public static Specification<Card> forListing(String keyword, Long seriesId) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(cb.isTrue(root.get("active")));
			predicates.add(cb.isTrue(root.get("series").get("active")));
			addKeyword(predicates, root, cb, keyword);
			if (seriesId != null) {
				predicates.add(cb.equal(root.get("series").get("id"), seriesId));
			}
			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	/** Dùng cho trang quản lý của Manager/Admin: lọc theo game, series, độ hiếm, trạng thái. */
	public static Specification<Card> forManagement(String keyword, Long gameId, Long seriesId,
			Rarity rarity, Boolean active) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			addKeyword(predicates, root, cb, keyword);
			if (gameId != null) {
				predicates.add(cb.equal(root.get("series").get("game").get("id"), gameId));
			}
			if (seriesId != null) {
				predicates.add(cb.equal(root.get("series").get("id"), seriesId));
			}
			if (rarity != null) {
				predicates.add(cb.equal(root.get("rarity"), rarity));
			}
			if (active != null) {
				predicates.add(cb.equal(root.get("active"), active));
			}
			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	// Tìm theo tên hoặc số thẻ, không phân biệt hoa thường
	private static void addKeyword(List<Predicate> predicates, Root<Card> root, CriteriaBuilder cb,
			String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return;
		}
		String pattern = "%" + keyword.trim().toLowerCase() + "%";
		predicates.add(cb.or(
				cb.like(cb.lower(root.get("name")), pattern),
				cb.like(cb.lower(root.get("cardNumber")), pattern)));
	}
}