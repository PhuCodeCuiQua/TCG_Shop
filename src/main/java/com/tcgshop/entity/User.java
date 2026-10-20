package com.tcgshop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users", indexes = {
		@Index(name = "idx_users_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

	@Column(name = "email", nullable = false, unique = true)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "full_name")
	private String fullName;

	@Column(name = "phone")
	private String phone;

	@Column(name = "avatar_url")
	private String avatarUrl;

	@Column(name = "avatar_public_id")
	private String avatarPublicId;

	@Column(name = "enabled", nullable = false)
	private boolean enabled = false;

	@Column(name = "locked", nullable = false)
	private boolean locked = false;

	@Column(name = "lock_reason")
	private String lockReason;

	@Column(name = "must_change_password", nullable = false)
	private boolean mustChangePassword = false;

	@Column(name = "failed_login_attempts", nullable = false)
	private int failedLoginAttempts = 0;

	@Column(name = "locked_until")
	private LocalDateTime lockedUntil;

	@Column(name = "deleted", nullable = false)
	private boolean deleted = false;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "user_roles",
			joinColumns = @JoinColumn(name = "user_id"),
			inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<Role> roles = new HashSet<>();
}
