package com.tcgshop.repository;

import com.tcgshop.entity.Shop;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopRepository extends JpaRepository<Shop, Long> {

	Optional<Shop> findByOwnerId(Long ownerId);

	boolean existsByName(String name);

	Optional<Shop> findBySlug(String slug);
}
