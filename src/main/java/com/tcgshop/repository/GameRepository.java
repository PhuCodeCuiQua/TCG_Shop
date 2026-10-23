package com.tcgshop.repository;

import com.tcgshop.entity.Game;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameRepository extends JpaRepository<Game, Long> {

	boolean existsByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

	boolean existsByCodeIgnoreCase(String code);

	boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

	// Tìm theo tên/mã, lọc theo trạng thái; keyword và active có thể null
	@Query("""
			select g
			from Game g
			where (:keyword is null
					or lower(g.name) like lower(concat('%', :keyword, '%'))
					or lower(g.code) like lower(concat('%', :keyword, '%')))
				and (:active is null or g.active = :active)
			""")
	Page<Game> search(
			@Param("keyword") String keyword,
			@Param("active") Boolean active,
			Pageable pageable);
}