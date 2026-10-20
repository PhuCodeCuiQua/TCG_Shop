package com.tcgshop.repository;

import com.tcgshop.entity.User;
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
				and (:enabled is null or u.enabled = :enabled)
				and (:locked is null or u.locked = :locked)
				and (:deleted is null or u.deleted = :deleted)
			""")
	Page<User> searchByKeywordAndStatus(
			@Param("keyword") String keyword,
			@Param("enabled") Boolean enabled,
			@Param("locked") Boolean locked,
			@Param("deleted") Boolean deleted,
			Pageable pageable);
}
