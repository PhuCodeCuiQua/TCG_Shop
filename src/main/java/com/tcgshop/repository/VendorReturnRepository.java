package com.tcgshop.repository;

import com.tcgshop.entity.ReturnRequest;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.ReturnStatus;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Truy vấn yêu cầu trả hàng giới hạn theo shop của Vendor. */
public interface VendorReturnRepository extends JpaRepository<ReturnRequest, Long> {

	@Query(value = """
			select r.order.id as orderId,
				r.order.orderCode as orderCode,
				r.user.fullName as customerName,
				r.reason as reason,
				r.proofImageUrl as proofImageUrl,
				r.createdAt as requestedAt
			from ReturnRequest r
			where r.order.shop.id = :shopId
				and r.order.status = :orderStatus
				and r.status = :returnStatus
			order by r.createdAt desc, r.id desc
			""",
			countQuery = """
			select count(r)
			from ReturnRequest r
			where r.order.shop.id = :shopId
				and r.order.status = :orderStatus
				and r.status = :returnStatus
			""")
	Page<VendorReturnRow> findPendingByShopId(
			@Param("shopId") Long shopId,
			@Param("orderStatus") OrderStatus orderStatus,
			@Param("returnStatus") ReturnStatus returnStatus,
			Pageable pageable);

	@Query("select case when count(r) > 0 then true else false end "
			+ "from ReturnRequest r where r.order.id = :orderId and r.order.shop.id = :shopId")
	boolean existsByOrderIdAndShopId(@Param("orderId") Long orderId, @Param("shopId") Long shopId);

	interface VendorReturnRow {
		Long getOrderId();

		String getOrderCode();

		String getCustomerName();

		String getReason();

		String getProofImageUrl();

		LocalDateTime getRequestedAt();
	}
}
