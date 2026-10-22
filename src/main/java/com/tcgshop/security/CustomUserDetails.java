package com.tcgshop.security;

import com.tcgshop.entity.Role;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.UserStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails implements UserDetails {

	private final Long id;
	private final String email;
	private final String password;
	private final UserStatus status;
	private final LocalDateTime lockedUntil;
	private final boolean mustChangePassword;
	private final List<SimpleGrantedAuthority> authorities;

	public CustomUserDetails(User user) {
		// Sao chép dữ liệu cần dùng để tránh giữ Entity và gặp lỗi lazy loading sau khi đóng session.
		this.id = user.getId();
		this.email = user.getEmail();
		this.password = user.getPassword();
		this.status = user.getStatus();
		this.lockedUntil = user.getLockedUntil();
		this.mustChangePassword = user.isMustChangePassword();
		this.authorities = user.getRoles().stream()
				.map(Role::getName)
				.map(roleName -> new SimpleGrantedAuthority(roleName.name()))
				.toList();
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public UserStatus getStatus() {
		return status;
	}

	public boolean isMustChangePassword() {
		return mustChangePassword;
	}

	public boolean getMustChangePassword() {
		return mustChangePassword;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return status != UserStatus.BLOCKED
				&& (lockedUntil == null || !lockedUntil.isAfter(LocalDateTime.now()));
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return status == UserStatus.ACTIVE;
	}
}
