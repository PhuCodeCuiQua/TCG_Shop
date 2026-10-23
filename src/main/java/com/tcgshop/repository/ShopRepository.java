package com.tcgshop.repository;

import com.tcgshop.entity.Shop;
import com.tcgshop.entity.enums.ShopStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShopRepository extends JpaRepository<Shop, Long> {

	Optional<Shop> findByOwnerId(Long ownerId);

	boolean existsByName(String name);

	Optional<Shop> findBySlug(String slug);

	@Query("select s from Shop s where s.deleted is null or s.deleted = false order by s.name asc")
	Page<Shop> findAllNotDeleted(Pageable pageable);

	@Query("select s from Shop s where s.deleted is null or s.deleted = false order by s.name asc")
	List<Shop> findAllNotDeletedForOptions();

	@Query(value = """
			select distinct s
			from Shop s
			left join fetch s.owner owner
			where (s.deleted is null or s.deleted = false)
				and (:status is null or s.status = :status)
				and (:keyword is null
					or lower(s.name) like lower(concat('%', :keyword, '%'))
					or lower(owner.email) like lower(concat('%', :keyword, '%'))
					or lower(owner.fullName) like lower(concat('%', :keyword, '%')))
			""",
			countQuery = """
					select count(s)
					from Shop s
					left join s.owner owner
					where (s.deleted is null or s.deleted = false)
						and (:status is null or s.status = :status)
						and (:keyword is null
							or lower(s.name) like lower(concat('%', :keyword, '%'))
							or lower(owner.email) like lower(concat('%', :keyword, '%'))
							or lower(owner.fullName) like lower(concat('%', :keyword, '%')))
					""")
	Page<Shop> searchForReview(
			@Param("status") ShopStatus status,
			@Param("keyword") String keyword,
			Pageable pageable);

	@Query("""
			select distinct s
			from Shop s
			left join fetch s.owner owner
			left join fetch owner.roles
			where s.id = :shopId
				and (s.deleted is null or s.deleted = false)
			""")
	Optional<Shop> findNotDeletedByIdWithOwnerRoles(@Param("shopId") Long shopId);
}
