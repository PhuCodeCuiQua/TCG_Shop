package com.tcgshop.repository;

import com.tcgshop.entity.Address;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AddressRepository extends JpaRepository<Address, Long> {

	@Query("select a from Address a where a.user.id = :userId "
			+ "order by a.isDefault desc, a.createdAt desc")
	List<Address> findByUserIdOrderByDefaultDescCreatedAtDesc(@Param("userId") Long userId);

	long countByUserId(Long userId);

	Optional<Address> findByIdAndUserId(Long id, Long userId);

	Optional<Address> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update Address a set a.isDefault = false where a.user.id = :userId and a.isDefault = true")
	int clearDefaultByUserId(@Param("userId") Long userId);
}
