package com.tcgshop.controller.rest;

import com.tcgshop.dto.request.ToggleUserStatusRequest;
import com.tcgshop.dto.response.UserManagementResponse;
import com.tcgshop.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class ManagerUserRestController {
    private final UserManagementService userManagementService;

    @PostMapping("/manager/users/{id}/toggle-status")
    public ResponseEntity<UserManagementResponse> toggleStatus(@PathVariable Long id,
            @Valid @RequestBody ToggleUserStatusRequest request) {
        return ResponseEntity.ok(userManagementService.toggleStatus(id, request));
    }
}
