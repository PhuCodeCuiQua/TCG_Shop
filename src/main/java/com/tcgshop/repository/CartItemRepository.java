package com.tcgshop.repository;

import com.tcgshop.entity.CartItem;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

	Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

	Optional<CartItem> findByIdAndCartUserId(Long id, Long userId);

	long countByCartId(Long cartId);

	@EntityGraph(attributePaths = {"product", "product.shop", "product.card"})
	List<CartItem> findAllByCartIdOrderByIdDesc(Long cartId);

	@Modifying
	@Query("delete from CartItem ci where ci.cart.id = :cartId and ci.product.id in :productIds")
	int deleteByCartIdAndProductIdIn(@Param("cartId") Long cartId,
			@Param("productIds") Collection<Long> productIds);
}
