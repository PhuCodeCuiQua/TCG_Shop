package com.tcgshop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "system_settings")
@Check(constraints = "singleton_key = 1")
@Getter
@Setter
@NoArgsConstructor
public class SystemSetting extends BaseEntity {
    @Column(name = "singleton_key", nullable = false, unique = true)
    private int singletonKey = 1;
    @Column(name = "site_name", nullable = false, length = 100)
    private String siteName;
    @Column(name = "logo_url", length = 1000)
    private String logoUrl;
    @Column(name = "logo_public_id")
    private String logoPublicId;
    @Column(name = "contact_phone", length = 20)
    private String contactPhone;
    @Column(name = "contact_email", length = 254)
    private String contactEmail;
}
