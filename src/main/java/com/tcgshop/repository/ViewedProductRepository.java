package com.tcgshop.repository;

import com.tcgshop.entity.ViewedProduct;
import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ViewedProductRepository extends JpaRepository<ViewedProduct, Long> {

	List<ViewedProduct> findByUserIdOrderByViewedAtDesc(Long userId);

	Optional<ViewedProduct> findByUserIdAndProductId(Long userId, Long productId);

	Page<ViewedProduct> findByUserId(Long userId, Pageable pageable);

	@Modifying
	@Query("delete from ViewedProduct viewed where viewed.user.id = :userId and viewed.id not in :keepIds")
	int deleteByUserIdKeepingIds(@Param("userId") Long userId, @Param("keepIds") Collection<Long> keepIds);
}
