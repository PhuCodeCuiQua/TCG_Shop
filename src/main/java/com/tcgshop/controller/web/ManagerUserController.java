package com.tcgshop.controller.web;

import com.tcgshop.dto.request.BlockUserRequest;
import com.tcgshop.dto.request.UserUpdateRequest;
import com.tcgshop.mapper.UserManagementMapper;
import com.tcgshop.dto.response.UserManagementResponse;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.entity.enums.UserStatus;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.UserManagementService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/manager/users")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
@RequiredArgsConstructor
public class ManagerUserController {

	private static final int PAGE_SIZE = 20;
	private static final String BASE_URL = "/manager/users";
	private static final String LIST_VIEW = "manager/users/list";
	private static final String DETAIL_VIEW = "manager/users/detail";

	private final UserManagementService userManagementService;
	private final CurrentUser currentUser;
	private final UserManagementMapper userManagementMapper;

	@GetMapping
	public String list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) RoleName role,
			@RequestParam(required = false) UserStatus status,
			@RequestParam(defaultValue = "0") int page,
			Model model) {
		Pageable pageable = PageRequest.of(
				Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
		model.addAttribute("usersPage", userManagementService.search(keyword, role, status, pageable));
		model.addAttribute("keyword", keyword);
		model.addAttribute("selectedRole", role);
		model.addAttribute("selectedStatus", status);
		model.addAttribute("roles", RoleName.values());
		model.addAttribute("statuses", UserStatus.values());
		model.addAttribute("currentUserId", currentUser.getId());
		addLayoutModel(model, "Quản lý người dùng");
		return LIST_VIEW;
	}

	@GetMapping("/{id}")
	public String detail(@PathVariable Long id, Model model) {
		prepareDetail(model, userManagementService.getById(id));
		return DETAIL_VIEW;
	}

	@GetMapping("/{id}/edit")
	public String edit(@PathVariable Long id, Model model) {
		UserManagementResponse user = userManagementService.getById(id);
		model.addAttribute("user", user);
		model.addAttribute("editUserRequest", userManagementMapper.toEditRequest(user));
		addEditModel(model, user.id());
		addLayoutModel(model, "Sửa người dùng");
		return "manager/users/edit";
	}

	@PostMapping("/{id}/edit")
	public String update(@PathVariable Long id,
			@Valid @ModelAttribute("editUserRequest") UserUpdateRequest request,
			BindingResult result, Model model, RedirectAttributes redirectAttributes) {
		UserManagementResponse user = userManagementService.getById(id);
		boolean roleEditable = currentUser.hasRole(RoleName.ROLE_ADMIN) && !id.equals(currentUser.getId());
		if (roleEditable && request.getRole() == null) {
			result.rejectValue("role", "user.role.required", "Vui lòng chọn vai trò");
		}
		if (result.hasErrors()) {
			model.addAttribute("user", user);
			addEditModel(model, id);
			addLayoutModel(model, "Sửa người dùng");
			return "manager/users/edit";
		}
		try {
			userManagementService.update(id, request);
		} catch (ConflictException exception) {
			result.reject("user.update.conflict", exception.getMessage());
			model.addAttribute("user", user);
			addEditModel(model, id);
			addLayoutModel(model, "Sửa người dùng");
			return "manager/users/edit";
		}
		redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật thông tin người dùng");
		return "redirect:" + BASE_URL;
	}

	private void addEditModel(Model model, Long userId) {
		model.addAttribute("roles", List.of(RoleName.ROLE_USER, RoleName.ROLE_VENDOR,
				RoleName.ROLE_SHIPPER, RoleName.ROLE_MANAGER, RoleName.ROLE_ADMIN));
		model.addAttribute("isAdmin", currentUser.hasRole(RoleName.ROLE_ADMIN));
		model.addAttribute("currentUserId", currentUser.getId());
		model.addAttribute("roleEditable", currentUser.hasRole(RoleName.ROLE_ADMIN)
				&& !userId.equals(currentUser.getId()));
	}

	@PostMapping("/{id}/block")
	public String block(
			@PathVariable Long id,
			@Valid @ModelAttribute("blockUserRequest") BlockUserRequest request,
			BindingResult result,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (!result.hasErrors()) {
			try {
				userManagementService.block(id, request);
				redirectAttributes.addFlashAttribute("successMessage", "Đã khóa tài khoản người dùng");
				return "redirect:" + BASE_URL + "/" + id;
			} catch (ConflictException exception) {
				String message = exception.getMessage() == null
						? "Không thể khóa tài khoản ở trạng thái hiện tại"
						: exception.getMessage();
				result.reject("user.block.conflict", message);
				model.addAttribute("formGlobalError", message);
			}
		}
		prepareDetail(model, userManagementService.getById(id));
		return DETAIL_VIEW;
	}

	@PostMapping("/{id}/unblock")
	public String unblock(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		userManagementService.unblock(id);
		redirectAttributes.addFlashAttribute("successMessage", "Đã mở khóa tài khoản người dùng");
		return "redirect:" + BASE_URL + "/" + id;
	}

	private void prepareDetail(Model model, UserManagementResponse user) {
		model.addAttribute("user", user);
		if (!model.containsAttribute("blockUserRequest")) {
			model.addAttribute("blockUserRequest", new BlockUserRequest());
		}
		model.addAttribute("canChangeLock", !user.id().equals(currentUser.getId()));
		addLayoutModel(model, "Chi tiết người dùng");
	}

	private void addLayoutModel(Model model, String title) {
		String role = currentUser.hasRole(RoleName.ROLE_ADMIN) ? "ADMIN" : "MANAGER";
		model.addAttribute("dashboardRole", role);
		model.addAttribute("dashboardTitle", title);
		model.addAttribute("currentPath", BASE_URL);
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Quản lý người dùng", "url", BASE_URL),
				Map.of("label", title, "url", BASE_URL)));
	}
}
