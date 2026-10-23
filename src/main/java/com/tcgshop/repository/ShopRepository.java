package com.tcgshop.repository;

import com.tcgshop.entity.Shop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ShopRepository extends JpaRepository<Shop, Long> {

	Optional<Shop> findByOwnerId(Long ownerId);

	boolean existsByName(String name);

	Optional<Shop> findBySlug(String slug);

	@Query("select s from Shop s where s.deleted is null or s.deleted = false order by s.name asc")
	Page<Shop> findAllNotDeleted(Pageable pageable);
}
