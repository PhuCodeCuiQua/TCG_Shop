package com.tcgshop.repository;

import com.tcgshop.dto.response.ShipperLoadResponse;
import com.tcgshop.entity.Order;
import com.tcgshop.entity.enums.OrderStatus;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Truy vấn đơn cho các màn hình của Manager/Admin (chỉ đọc). */
public interface ManagerOrderRepository extends Repository<Order, Long> {

	// Đơn ở trạng thái :status, đã hoặc chưa có shipper, tìm theo mã đơn hoặc tên người nhận
	@EntityGraph(attributePaths = {"shop", "shipper"})
	@Query("""
			select o
			from com.tcgshop.entity.Order o
			where o.status = :status
				and ((:assigned = true and o.shipper is not null)
					or (:assigned = false and o.shipper is null))
				and (:keyword is null
					or lower(o.orderCode) like lower(concat('%', :keyword, '%'))
					or lower(o.receiverName) like lower(concat('%', :keyword, '%')))
			""")
	Page<Order> searchByStatusAndAssigned(
			@Param("status") OrderStatus status,
			@Param("assigned") boolean assigned,
			@Param("keyword") String keyword,
			Pageable pageable);

	// Shipper còn hoạt động kèm số đơn đang giao của từng người
	@Query("""
			select new com.tcgshop.dto.response.ShipperLoadResponse(
				u.id, u.fullName, u.email,
				(select count(o2)
					from com.tcgshop.entity.Order o2
					where o2.shipper = u and o2.status = :shippingStatus))
			from User u join u.roles r
			where r.name = :role and u.status = :userStatus
			order by u.fullName
			""")
	List<ShipperLoadResponse> findShipperLoads(
			@Param("role") RoleName role,
			@Param("userStatus") UserStatus userStatus,
			@Param("shippingStatus") OrderStatus shippingStatus);
}