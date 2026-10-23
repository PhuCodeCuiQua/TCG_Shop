package com.tcgshop.repository;

import com.tcgshop.entity.ShippingCarrier;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ShippingCarrierRepository extends JpaRepository<ShippingCarrier, Long> {

	List<ShippingCarrier> findAllByActiveTrueOrderByNameAsc();

	boolean existsByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

	boolean existsByCodeIgnoreCase(String code);

	boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

	@Query("select count(o) from Order o where o.shippingCarrier.id = :carrierId")
	long countOrdersUsingCarrier(@Param("carrierId") Long carrierId);
}
