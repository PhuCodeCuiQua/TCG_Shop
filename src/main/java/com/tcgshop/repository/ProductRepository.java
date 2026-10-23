package com.tcgshop.repository;

import com.tcgshop.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

	// Dùng để quyết định ẩn hay xóa Series: hàng niêm phong gắn trực tiếp với series (PA-10)
	boolean existsBySeriesId(Long seriesId);
}