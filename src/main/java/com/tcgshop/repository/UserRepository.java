package com.tcgshop.repository;

import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

	@EntityGraph(attributePaths = "roles")
	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	@Query("""
			select u
			from User u
			where (:keyword is null
					or lower(u.email) like lower(concat('%', :keyword, '%'))
					or lower(u.fullName) like lower(concat('%', :keyword, '%')))
				and (:status is null or u.status = :status)
				and (:roleName is null or exists (
					select r
					from u.roles r
					where r.name = :roleName
				))
			""")
	Page<User> searchByKeywordAndStatusAndRole(
			@Param("keyword") String keyword,
			@Param("status") UserStatus status,
			@Param("roleName") RoleName roleName,
			Pageable pageable);
}
