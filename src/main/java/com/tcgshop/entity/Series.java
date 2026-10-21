package com.tcgshop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "series", uniqueConstraints = {
		@UniqueConstraint(name = "uk_series_game_code", columnNames = {"game_id", "code"})
})
@Getter
@Setter
@NoArgsConstructor
public class Series extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "game_id")
	private Game game;

	@Column(name = "name")
	private String name;

	@Column(name = "code")
	private String code;

	@Column(name = "release_date")
	private LocalDate releaseDate;

	@Column(name = "active")
	private boolean active = true;
}
