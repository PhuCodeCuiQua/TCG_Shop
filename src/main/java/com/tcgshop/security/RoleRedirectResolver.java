package com.tcgshop.security;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class RoleRedirectResolver {
	public String resolveTargetUrl(Collection<? extends GrantedAuthority> authorities) {
		if (authorities != null) {
			if (hasRole(authorities, "ROLE_ADMIN") || hasRole(authorities, "ROLE_MANAGER")) return "/manager/users";
			if (hasRole(authorities, "ROLE_SHIPPER")) return "/shipper/dashboard";
			if (hasRole(authorities, "ROLE_VENDOR")) return "/vendor/dashboard";
		}
		return "/";
	}

	public boolean isSafeReturnUrl(String returnUrl) {
		return returnUrl != null && returnUrl.startsWith("/") && !returnUrl.startsWith("//")
				&& !returnUrl.contains("http") && !returnUrl.contains("\\")
				&& !returnUrl.matches(".*[\\x00-\\x1F].*");
	}

	private boolean hasRole(Collection<? extends GrantedAuthority> authorities, String role) {
		return authorities.stream().anyMatch(authority -> role.equals(authority.getAuthority()));
	}
}
