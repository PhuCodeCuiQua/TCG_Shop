package com.tcgshop.repository;

import static org.assertj.core.api.Assertions.*;
import com.tcgshop.entity.SystemSetting;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest(properties = {"spring.datasource.url=jdbc:h2:mem:system-settings;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.sql.init.mode=never"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SystemSettingRepositoryTest {
    @Autowired private SystemSettingRepository repository;
    @Test void repeatedInitializationAndUpdatesKeepOneRow() {
        repository.initializeIfAbsent(); repository.initializeIfAbsent();
        var setting = repository.findSingletonForUpdate().orElseThrow();
        Long id = setting.getId(); setting.setSiteName("Persisted Market"); repository.saveAndFlush(setting);
        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findBySingletonKey(1).orElseThrow().getSiteName()).isEqualTo("Persisted Market");
        assertThat(repository.findSingletonForUpdate().orElseThrow().getId()).isEqualTo(id);
    }
    @Test void databaseRejectsSecondConfiguration() {
        repository.initializeIfAbsent();
        var duplicate = new SystemSetting(); duplicate.setSiteName("Duplicate");
        assertThatThrownBy(() -> repository.saveAndFlush(duplicate)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
    @Test void databaseRejectsAnotherSingletonKey() {
        var other = new SystemSetting(); other.setSiteName("Other"); other.setSingletonKey(2);
        assertThatThrownBy(() -> repository.saveAndFlush(other)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
