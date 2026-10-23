package com.tcgshop.repository;

import com.tcgshop.entity.CommissionSetting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommissionSettingRepository extends JpaRepository<CommissionSetting, Long> {

	Optional<CommissionSetting> findTopByOrderByCreatedAtDesc();

	Page<CommissionSetting> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
