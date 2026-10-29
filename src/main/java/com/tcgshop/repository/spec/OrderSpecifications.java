package com.tcgshop.repository.spec;

import com.tcgshop.entity.Order;
import com.tcgshop.entity.enums.OrderStatus;
import java.util.Collection;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class OrderSpecifications {
	private static final char LIKE_ESCAPE = '\\';

	private OrderSpecifications() { }

	public static Specification<Order> belongsToUser(Long userId) {
		return (root, query, builder) -> builder.equal(root.get("user").get("id"), userId);
	}

	public static Specification<Order> statusIn(Collection<OrderStatus> statuses) {
		if (statuses == null || statuses.isEmpty()) return null;
		return (root, query, builder) -> root.get("status").in(statuses);
	}

	public static Specification<Order> codeContains(String keyword) {
		if (keyword == null || keyword.isBlank()) return null;
		String escaped = escapeLike(keyword.toLowerCase(Locale.ROOT));
		return (root, query, builder) -> builder.like(
				builder.lower(root.get("orderCode")), "%" + escaped + "%", LIKE_ESCAPE);
	}

	static String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
