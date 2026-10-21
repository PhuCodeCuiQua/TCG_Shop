package com.tcgshop.entity;

import com.tcgshop.entity.enums.MediaType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "review_media")
@Getter
@Setter
@NoArgsConstructor
public class ReviewMedia extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "review_id")
	private Review review;

	@Column(name = "media_url")
	private String mediaUrl;

	@Column(name = "media_public_id")
	private String mediaPublicId;

	@Enumerated(EnumType.STRING)
	@Column(name = "media_type")
	private MediaType mediaType;
}
