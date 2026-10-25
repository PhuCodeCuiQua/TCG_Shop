package com.tcgshop.service.impl;

import com.tcgshop.config.ReconciliationProperties;
import com.tcgshop.dto.response.ReconciliationCheckSummaryResponse;
import com.tcgshop.dto.response.ReconciliationFindingResponse;
import com.tcgshop.dto.response.ReconciliationReportResponse;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.ReconciliationSeverity;
import com.tcgshop.service.OrderReconciliationService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderReconciliationServiceImpl implements OrderReconciliationService {
	private final NamedParameterJdbcTemplate jdbc;
	private final ReconciliationProperties properties;
	private final Clock clock;

	public OrderReconciliationServiceImpl(NamedParameterJdbcTemplate jdbc,
			ReconciliationProperties properties, Clock clock) {
		this.jdbc = jdbc;
		this.properties = properties;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public ReconciliationReportResponse reconcile() {
		List<ReconciliationCheckSummaryResponse> summaries = new ArrayList<>();
		List<ReconciliationFindingResponse> findings = new ArrayList<>();
		long errors = 0;
		long warnings = 0;
		int limit = Math.max(0, properties.maxFindingsPerCheck());

		for (Check check : checks()) {
			Long countValue = jdbc.queryForObject("SELECT COUNT(*) FROM (" + check.selectSql() + ") t",
					Map.of(), Long.class);
			long count = countValue == null ? 0 : countValue;
			summaries.add(new ReconciliationCheckSummaryResponse(check.code(), check.title(), check.severity(), count));
			if (check.severity() == ReconciliationSeverity.ERROR) {
				errors += count;
			} else {
				warnings += count;
			}
			if (limit > 0 && count > 0) {
				String limitedSql = "SELECT entity_id, entity_code, detail FROM (" + check.selectSql()
						+ ") findings ORDER BY entity_id LIMIT :findingLimit";
				List<ReconciliationFindingResponse> rows = jdbc.query(limitedSql,
						Map.of("findingLimit", limit), (rs, rowNum) -> new ReconciliationFindingResponse(
								check.code(), check.severity(), check.entityType(), rs.getLong("entity_id"),
								rs.getString("entity_code"), rs.getString("detail")));
				findings.addAll(rows);
			}
		}
		Long ordersValue = jdbc.queryForObject("SELECT COUNT(*) FROM orders", Map.of(), Long.class);
		return new ReconciliationReportResponse(LocalDateTime.now(clock), ordersValue == null ? 0 : ordersValue,
				errors, warnings, summaries, findings);
	}

	private List<Check> checks() {
		// SQL đối soát được viết độc lập với các service ghi dữ liệu để tránh lặp lại cùng một lỗi tính toán.
		String soldStatuses = sqlStatuses(OrderStatus.SOLD_COUNT_STATUSES);
		String commissionStatuses = sqlStatuses(OrderStatus.COMMISSION_FINALIZED_STATUSES);
		String nonCommissionStatuses = sqlStatuses(EnumSet.complementOf(
				EnumSet.copyOf(OrderStatus.COMMISSION_FINALIZED_STATUSES)));
		return List.of(
				check("ORD_TOTAL_FORMULA", "Đơn: tổng tiền không khớp công thức", ReconciliationSeverity.ERROR,
						"ORDER", "SELECT id AS entity_id, order_code AS entity_code, 'total_amount != subtotal - discount_amount + shipping_fee' AS detail FROM orders WHERE total_amount IS NULL OR subtotal IS NULL OR discount_amount IS NULL OR shipping_fee IS NULL OR ABS(total_amount - (subtotal - discount_amount + shipping_fee)) > 0.001"),
				check("ORD_NEGATIVE", "Đơn: số tiền âm hoặc chiết khấu không hợp lệ", ReconciliationSeverity.ERROR,
						"ORDER", "SELECT id AS entity_id, order_code AS entity_code, 'negative amount or invalid discount allocation' AS detail FROM orders WHERE subtotal < 0 OR discount_amount < 0 OR shipping_fee < 0 OR total_amount < 0 OR discount_amount > subtotal OR COALESCE(platform_discount_amount, 0) > discount_amount"),
				check("ORD_NO_ITEMS", "Đơn: không có dòng hàng", ReconciliationSeverity.ERROR,
						"ORDER", "SELECT o.id AS entity_id, o.order_code AS entity_code, 'order has no order_items' AS detail FROM orders o WHERE NOT EXISTS (SELECT 1 FROM order_items i WHERE i.order_id = o.id)"),
				check("ORD_ITEMS_SUM", "Đơn: subtotal không khớp tổng dòng hàng", ReconciliationSeverity.ERROR,
						"ORDER", "SELECT o.id AS entity_id, o.order_code AS entity_code, 'subtotal != SUM(order_items.total_price)' AS detail FROM orders o WHERE o.subtotal IS NULL OR ABS(o.subtotal - COALESCE((SELECT SUM(i.total_price) FROM order_items i WHERE i.order_id = o.id), 0)) > 0.001"),
				check("ITEM_LINE", "Dòng hàng: số lượng hoặc thành tiền sai", ReconciliationSeverity.ERROR,
						"ORDER_ITEM", "SELECT i.id AS entity_id, o.order_code AS entity_code, 'quantity <= 0 or total_price != price * quantity' AS detail FROM order_items i LEFT JOIN orders o ON o.id = i.order_id WHERE i.quantity <= 0 OR i.total_price IS NULL OR i.price IS NULL OR ABS(i.total_price - (i.price * i.quantity)) > 0.001"),
				check("GROUP_TOTAL", "Nhóm đơn: tổng tiền không khớp đơn con", ReconciliationSeverity.ERROR,
						"ORDER_GROUP", "SELECT g.id AS entity_id, NULL AS entity_code, 'group total_amount != sum of child order totals' AS detail FROM order_groups g LEFT JOIN orders o ON o.order_group_id = g.id GROUP BY g.id, g.total_amount HAVING COUNT(o.id) = 0 OR g.total_amount IS NULL OR ABS(g.total_amount - COALESCE(SUM(o.total_amount), 0)) > 0.001"),
				// ROUND trên DECIMAL của MySQL giữ phép làm tròn nửa lên tương ứng HALF_UP mà không đổi sang DOUBLE/FLOAT.
				check("COMM_FORMULA", "Hoa hồng: snapshot thiếu hoặc không khớp công thức", ReconciliationSeverity.ERROR,
						"ORDER", "SELECT o.id AS entity_id, o.order_code AS entity_code, 'commission snapshot is missing or inconsistent' AS detail FROM orders o WHERE o.status IN (" + commissionStatuses + ") AND (o.commission_rate IS NULL OR o.commission_amount IS NULL OR o.shop_revenue IS NULL OR o.subtotal IS NULL OR o.discount_amount IS NULL OR ABS(o.commission_amount - ROUND((o.subtotal - GREATEST(0, o.discount_amount - COALESCE(o.platform_discount_amount, 0))) * o.commission_rate / 100, 0)) > 0.001 OR ABS(o.commission_amount + o.shop_revenue - (o.subtotal - GREATEST(0, o.discount_amount - COALESCE(o.platform_discount_amount, 0)))) > 0.001 OR o.shop_revenue < 0)"),
				check("COMM_PREMATURE", "Hoa hồng: snapshot tồn tại trước khi chốt", ReconciliationSeverity.ERROR,
						"ORDER", "SELECT id AS entity_id, order_code AS entity_code, 'commission snapshot exists before finalized status' AS detail FROM orders WHERE status IN (" + nonCommissionStatuses + ") AND (commission_rate IS NOT NULL OR commission_amount IS NOT NULL OR shop_revenue IS NOT NULL)"),
				check("REFUND_FORMULA", "Hoàn tiền: số tiền hoặc thời điểm hoàn sai", ReconciliationSeverity.ERROR,
						"ORDER", "SELECT id AS entity_id, order_code AS entity_code, 'refund fields are inconsistent with order status' AS detail FROM orders WHERE (status = 'REFUNDED' AND (refund_amount IS NULL OR total_amount IS NULL OR ABS(refund_amount - total_amount) > 0.001 OR refunded_at IS NULL)) OR (refund_amount IS NOT NULL AND status NOT IN ('REFUNDED', 'CANCELLED'))"),
				check("MONEY_SCALE", "Tiền VND: giá trị có phần thập phân", ReconciliationSeverity.WARNING,
						"ORDER", "SELECT id AS entity_id, order_code AS entity_code, 'one or more VND values have a fractional part' AS detail FROM orders WHERE subtotal <> ROUND(subtotal, 0) OR discount_amount <> ROUND(discount_amount, 0) OR shipping_fee <> ROUND(shipping_fee, 0) OR total_amount <> ROUND(total_amount, 0) OR commission_amount <> ROUND(commission_amount, 0) OR shop_revenue <> ROUND(shop_revenue, 0)"),
				check("HIST_MISSING", "Lịch sử: đơn không có dòng trạng thái", ReconciliationSeverity.WARNING,
						"ORDER", "SELECT o.id AS entity_id, o.order_code AS entity_code, 'order has no order_status_histories' AS detail FROM orders o WHERE NOT EXISTS (SELECT 1 FROM order_status_histories h WHERE h.order_id = o.id)"),
				check("HIST_MISMATCH", "Lịch sử: trạng thái mới nhất khác đơn", ReconciliationSeverity.WARNING,
						"ORDER", "SELECT o.id AS entity_id, o.order_code AS entity_code, 'latest history new_status != order status' AS detail FROM orders o JOIN order_status_histories h ON h.order_id = o.id AND h.id = (SELECT MAX(h2.id) FROM order_status_histories h2 WHERE h2.order_id = o.id) WHERE h.new_status IS NULL OR h.new_status <> o.status"),
				check("SOLD_COUNT_LOW", "Sản phẩm: sold_count thấp hơn số lượng đã bán", ReconciliationSeverity.ERROR,
						"PRODUCT", soldCountSql(soldStatuses, "<", "sold_count below qualifying order item quantity")),
				check("SOLD_COUNT_HIGH", "Sản phẩm: sold_count cao hơn số lượng đã bán", ReconciliationSeverity.WARNING,
						"PRODUCT", soldCountSql(soldStatuses, ">", "sold_count may include seed or legacy sales")),
				check("STOCK_NEGATIVE", "Sản phẩm: tồn kho âm", ReconciliationSeverity.ERROR,
						"PRODUCT", "SELECT id AS entity_id, NULL AS entity_code, 'stock < 0' AS detail FROM products WHERE stock < 0"),
				check("COUPON_USED_LOW", "Mã giảm giá: used_quantity thấp hơn lượt sử dụng", ReconciliationSeverity.ERROR,
						"COUPON", couponCountSql("<", "used_quantity below coupon_usages count")),
				check("COUPON_USED_HIGH", "Mã giảm giá: used_quantity cao hơn lượt sử dụng", ReconciliationSeverity.WARNING,
						"COUPON", couponCountSql(">", "used_quantity may include seed or legacy uses")),
				check("COUPON_USAGE_AMOUNT", "Lượt dùng mã: số tiền giảm không khớp đơn", ReconciliationSeverity.ERROR,
						"COUPON_USAGE", "SELECT cu.id AS entity_id, o.order_code AS entity_code, 'coupon usage discount does not match order allocation' AS detail FROM coupon_usages cu JOIN coupons c ON c.id = cu.coupon_id JOIN orders o ON o.id = cu.order_id WHERE ABS(cu.discount_amount - CASE WHEN c.shop_id IS NULL THEN COALESCE((SELECT SUM(COALESCE(o2.platform_discount_amount, 0)) FROM orders o2 WHERE o2.order_group_id = o.order_group_id), 0) ELSE (o.discount_amount - COALESCE(o.platform_discount_amount, 0)) END) > 0.001")
		);
	}

	// sold_count lớn hơn số lượng đơn hiện biết chỉ là cảnh báo vì có thể phản ánh seed hoặc lịch sử trước khi tracking hoạt động.
	private String soldCountSql(String statuses, String comparator, String detail) {
		return "SELECT p.id AS entity_id, NULL AS entity_code, '" + detail
				+ "' AS detail FROM products p LEFT JOIN order_items i ON i.product_id = p.id "
				+ "LEFT JOIN orders o ON o.id = i.order_id AND o.status IN (" + statuses + ") "
				+ "GROUP BY p.id, p.sold_count HAVING p.sold_count " + comparator
				+ " COALESCE(SUM(CASE WHEN o.id IS NOT NULL THEN i.quantity ELSE 0 END), 0)";
	}

	private String couponCountSql(String comparator, String detail) {
		return "SELECT c.id AS entity_id, NULL AS entity_code, '" + detail
				+ "' AS detail FROM coupons c LEFT JOIN coupon_usages cu ON cu.coupon_id = c.id "
				+ "GROUP BY c.id, c.used_quantity HAVING c.used_quantity " + comparator + " COUNT(cu.id)";
	}

	private Check check(String code, String title, ReconciliationSeverity severity, String entityType, String sql) {
		return new Check(code, title, severity, entityType, sql);
	}

	private String sqlStatuses(Set<OrderStatus> statuses) {
		return java.util.Arrays.stream(OrderStatus.values())
				.filter(statuses::contains)
				.map(status -> "'" + status.name() + "'")
				.collect(java.util.stream.Collectors.joining(", "));
	}

	private record Check(String code, String title, ReconciliationSeverity severity, String entityType,
			String selectSql) {
	}
}
