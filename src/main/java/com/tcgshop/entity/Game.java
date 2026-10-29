package com.tcgshop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "games")
@Getter
@Setter
@NoArgsConstructor
public class Game extends BaseEntity {

	@Column(name = "name", unique = true)
	private String name;

	@Column(name = "code", unique = true)
	private String code;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "logo_url")
	private String logoUrl;

	@Column(name = "logo_public_id")
	private String logoPublicId;

	@Column(name = "active")
	private boolean active = true;
}
