package com.tcgshop.repository;

import com.tcgshop.entity.SystemSetting;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {
    Optional<SystemSetting> findBySingletonKey(int singletonKey);

    @Modifying
    @Query(value = "insert ignore into system_settings (singleton_key, site_name, created_at, updated_at) "
            + "values (1, 'TCGSHOP', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", nativeQuery = true)
    void initializeIfAbsent();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SystemSetting s where s.singletonKey = 1")
    Optional<SystemSetting> findSingletonForUpdate();
}
