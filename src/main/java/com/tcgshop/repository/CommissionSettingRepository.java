package com.tcgshop.repository;

import com.tcgshop.entity.CommissionSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommissionSettingRepository extends JpaRepository<CommissionSetting, Long> {

	Optional<CommissionSetting> findTopByOrderByCreatedAtDesc();
}
