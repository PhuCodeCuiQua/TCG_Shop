package com.tcgshop.security;

import com.tcgshop.entity.enums.RoleName;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

	public boolean isAuthenticated() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null
				&& authentication.isAuthenticated()
				&& !(authentication instanceof AnonymousAuthenticationToken);
	}

	public Long getId() {
		return getUserDetails().getId();
	}

	public String getEmail() {
		return getUserDetails().getEmail();
	}

	public Set<RoleName> getRoles() {
		return getUserDetails().getAuthorities().stream()
				.map(authority -> RoleName.valueOf(authority.getAuthority()))
				.collect(Collectors.toUnmodifiableSet());
	}

	public boolean hasRole(RoleName role) {
		return getUserDetails().getAuthorities().contains(new SimpleGrantedAuthority(role.name()));
	}

	private CustomUserDetails getUserDetails() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!isAuthenticated() || authentication == null
				|| !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
			throw new AuthenticationCredentialsNotFoundException("No authenticated user is available");
		}
		return userDetails;
	}
}
