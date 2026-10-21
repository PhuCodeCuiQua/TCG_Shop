package com.tcgshop.repository;

import com.tcgshop.entity.ViewedProduct;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ViewedProductRepository extends JpaRepository<ViewedProduct, Long> {

	List<ViewedProduct> findByUserIdOrderByViewedAtDesc(Long userId);
}
