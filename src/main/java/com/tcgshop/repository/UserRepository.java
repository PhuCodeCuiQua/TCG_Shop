package com.tcgshop.repository;

import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

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
				and (:excludePrivileged = false or not exists (
					select r
					from u.roles r
					where r.name = :excludedRoleOne or r.name = :excludedRoleTwo
				))
			""")
	Page<User> searchByKeywordAndStatusAndRole(
			@Param("keyword") String keyword,
			@Param("status") UserStatus status,
			@Param("roleName") RoleName roleName,
			@Param("excludePrivileged") boolean excludePrivileged,
			@Param("excludedRoleOne") RoleName excludedRoleOne,
			@Param("excludedRoleTwo") RoleName excludedRoleTwo,
			Pageable pageable);

	@Query("select u from User u left join fetch u.roles where u.id = :id")
	Optional<User> findByIdWithRoles(@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from User u where u.id = :id")
	Optional<User> findByIdForUpdate(@Param("id") Long id);

	@Query("select distinct u from User u left join fetch u.roles where u.id in :ids")
	List<User> findAllWithRolesByIdIn(@Param("ids") Collection<Long> ids);

	@Query("select distinct u.id from User u join u.roles r where r.name in :roleNames")
	List<Long> findIdsByRoleNamesIn(@Param("roleNames") Collection<RoleName> roleNames);
}
