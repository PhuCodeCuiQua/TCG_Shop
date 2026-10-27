package com.tcgshop.controller.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.tcgshop.config.PasswordEncoderConfig;
import com.tcgshop.config.SecurityConfig;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.entity.Role;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.exception.GlobalExceptionHandler;
import com.tcgshop.security.CookieUtil;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.security.CustomUserDetails;
import com.tcgshop.security.JsonAccessDeniedHandler;
import com.tcgshop.security.JsonAuthenticationEntryPoint;
import com.tcgshop.security.JwtProvider;
import com.tcgshop.security.UserDetailsServiceImpl;
import com.tcgshop.service.RefreshTokenService;
import com.tcgshop.service.UserManagementService;
import com.tcgshop.mapper.UserManagementMapperImpl;
import com.tcgshop.controller.rest.ManagerUserRestController;
import com.tcgshop.dto.response.UserManagementResponse;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {ManagerUserController.class, ManagerUserRestController.class}, properties = {
    "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
    "app.jwt.access-expiration-minutes=30", "app.jwt.refresh-expiration-days=7",
    "app.jwt.access-cookie-name=ACCESS_TOKEN", "app.jwt.refresh-cookie-name=REFRESH_TOKEN",
    "app.cookie.secure=false"
})
@Import({SecurityConfig.class, JwtProvider.class, CookieUtil.class, PasswordEncoderConfig.class,
    JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class, GlobalExceptionHandler.class,
    CurrentUser.class, UserManagementMapperImpl.class})
class ManagerUserControllerMvcTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtProvider jwtProvider;
    @MockBean private UserManagementService userManagementService;
    @MockBean private UserDetailsServiceImpl userDetailsService;
    @MockBean private RefreshTokenService refreshTokenService;

    @BeforeEach
    void configureUsers() {
        when(userDetailsService.loadUserByUsername(anyString())).thenAnswer(invocation ->
            details(invocation.getArgument(0)));
    }

    @Test
    void listAndEditRenderWithRealMapperAndDashboard() throws Exception {
        when(userManagementService.search(any(), any(), any(), any())).thenReturn(
            new PageResponse<>(List.of(response(20L), response(42L)), 0, 20, 2, 1, true, true));
        when(userManagementService.getById(20L)).thenReturn(response(20L));
        mockMvc.perform(get("/manager/users").cookie(cookie("admin")))
            .andExpect(status().isOk()).andExpect(view().name("manager/users/list"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("/manager/users/20/toggle-status")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("/manager/users/42/toggle-status"))));
        mockMvc.perform(get("/manager/users/20/edit").cookie(cookie("manager")))
            .andExpect(status().isOk()).andExpect(view().name("manager/users/edit"));
    }

    @Test
    void invalidEditKeepsFormAndDoesNotSave() throws Exception {
        when(userManagementService.getById(20L)).thenReturn(response(20L));
        mockMvc.perform(post("/manager/users/20/edit").cookie(cookie("manager")).with(csrf())
            .param("fullName", "").param("phoneNumber", "123"))
            .andExpect(status().isOk()).andExpect(view().name("manager/users/edit"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model()
                .attributeHasFieldErrors("editUserRequest", "fullName", "phoneNumber"));
        verify(userManagementService, never()).update(any(), any());
    }

    @Test
    void validEditRedirectsWithSuccessToast() throws Exception {
        when(userManagementService.getById(20L)).thenReturn(response(20L));
        mockMvc.perform(post("/manager/users/20/edit").cookie(cookie("admin")).with(csrf())
            .param("fullName", "Nguyễn Văn An").param("phoneNumber", "0901234567")
            .param("email", "tampered@example.com").param("role", "ROLE_ADMIN"))
            .andExpect(status().is3xxRedirection())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/manager/users"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attributeExists("successMessage"));
        verify(userManagementService).update(org.mockito.ArgumentMatchers.eq(20L), any());
    }

    @Test
    void toggleRequiresCsrfAndManagerRole() throws Exception {
        mockMvc.perform(post("/manager/users/20/toggle-status").cookie(cookie("admin"))
            .contentType("application/json").accept("application/json")
            .content("{\"expectedStatus\":\"ACTIVE\",\"reason\":\"Vi phạm\"}"))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/manager/users/20/toggle-status").cookie(cookie("user")).with(csrf())
            .contentType("application/json").accept("application/json")
            .content("{\"expectedStatus\":\"ACTIVE\",\"reason\":\"Vi phạm\"}"))
            .andExpect(status().isForbidden());
        verify(userManagementService, never()).toggleStatus(any(), any());
    }

    @Test
    void toggleReturnsJsonAndValidatesRequest() throws Exception {
        when(userManagementService.toggleStatus(any(), any())).thenReturn(response(20L));
        mockMvc.perform(post("/manager/users/20/toggle-status").cookie(cookie("manager")).with(csrf())
            .contentType("application/json").accept("application/json")
            .content("{\"expectedStatus\":\"BLOCKED\"}"))
            .andExpect(status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status").value("ACTIVE"));
        mockMvc.perform(post("/manager/users/20/toggle-status").cookie(cookie("manager")).with(csrf())
            .contentType("application/json").accept("application/json").content("{}"))
            .andExpect(status().isBadRequest());
    }

    private UserManagementResponse response(Long id) {
        return new UserManagementResponse(id, "Nguyễn Văn An", "user@example.com", "0901234567",
            List.of(RoleName.ROLE_USER), UserStatus.ACTIVE, "Đang hoạt động", true, null, null, null);
    }
    private Cookie cookie(String role) {
        return new Cookie("ACCESS_TOKEN", jwtProvider.generateAccessToken(details(role + "@example.com")));
    }
    private CustomUserDetails details(String email) {
        User user = new User();
        user.setId(42L);
        user.setEmail(email);
        user.setFullName("Nguyễn Văn An");
        user.setPassword("encoded-password");
        user.setStatus(UserStatus.ACTIVE);
        Role role = new Role();
        role.setName(RoleName.valueOf("ROLE_" + email.substring(0, email.indexOf('@')).toUpperCase()));
        user.setRoles(java.util.Set.of(role));
        return new CustomUserDetails(user);
    }
}
