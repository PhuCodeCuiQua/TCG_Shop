package com.tcgshop.repository;

import com.tcgshop.entity.CartItem;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

	Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

	Optional<CartItem> findByIdAndCartUserId(Long id, Long userId);

	long countByCartId(Long cartId);

	@EntityGraph(attributePaths = {"product", "product.shop", "product.card"})
	List<CartItem> findAllByCartIdOrderByIdDesc(Long cartId);
}
