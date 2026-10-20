package com.tcgshop.repository;

import com.tcgshop.entity.Address;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Long> {

	List<Address> findByUserId(Long userId);

	long countByUserId(Long userId);

	Optional<Address> findByIdAndUserId(Long id, Long userId);
}
