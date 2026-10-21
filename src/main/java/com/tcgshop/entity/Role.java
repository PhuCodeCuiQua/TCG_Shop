package com.tcgshop.entity;

import com.tcgshop.entity.enums.RoleName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role extends BaseEntity {

	@Enumerated(EnumType.STRING)
	@Column(name = "name", unique = true)
	private RoleName name;

	@Column(name = "description")
	private String description;
}
