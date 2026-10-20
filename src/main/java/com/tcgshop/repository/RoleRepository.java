package com.tcgshop.repository;

import com.tcgshop.entity.Role;
import com.tcgshop.entity.enums.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

	Optional<Role> findByName(RoleName name);
}
