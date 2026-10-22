package com.tcgshop.controller.web;

import com.tcgshop.dto.request.GameRequest;
import com.tcgshop.dto.response.GameResponse;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.GameCatalogService;
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

/**
 * Quản lý Game (PA-10). Manager và Admin dùng chung (URL /manager/** đã cho cả hai role ở SecurityConfig).
 * Lớp phân quyền thứ hai: @PreAuthorize. Không có ownership vì đây là dữ liệu danh mục dùng chung.
 */
@Controller
@RequestMapping("/manager/games")
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
@RequiredArgsConstructor
public class GameCatalogController {

	private static final int PAGE_SIZE = 10;
	private static final String BASE_URL = "/manager/games";
	private static final String LIST_VIEW = "manager/games/list";
	private static final String FORM_VIEW = "manager/games/form";

	private final GameCatalogService gameCatalogService;
	private final CurrentUser currentUser;

	@GetMapping
	public String list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Boolean active,
			@RequestParam(defaultValue = "0") int page,
			Model model) {
		Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by("name").ascending());
		model.addAttribute("games", gameCatalogService.search(keyword, active, pageable));
		// Giữ tham số lọc trên URL/giao diện khi chuyển trang (mục 13)
		model.addAttribute("keyword", keyword);
		model.addAttribute("active", active);
		addLayoutModel(model, "Quản lý game");
		return LIST_VIEW;
	}

	@GetMapping("/new")
	public String newForm(Model model) {
		model.addAttribute("gameRequest", new GameRequest(null, null, null));
		prepareForm(model, "Thêm game", BASE_URL);
		return FORM_VIEW;
	}

	@PostMapping
	public String create(
			@Valid @ModelAttribute("gameRequest") GameRequest request,
			BindingResult result,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (!result.hasErrors()) {
			try {
				gameCatalogService.create(request);
				redirectAttributes.addFlashAttribute("successMessage", "Đã thêm game");
				return "redirect:" + BASE_URL;
			} catch (ConflictException ex) {
				// Chỉ bắt lỗi trùng để hiện lên form và giữ dữ liệu đã nhập; lỗi khác vẫn do GlobalExceptionHandler xử lý
				result.reject("game.conflict", ex.getMessage());
			}
		}
		prepareForm(model, "Thêm game", BASE_URL);
		return FORM_VIEW;
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		GameResponse game = gameCatalogService.getById(id);
		model.addAttribute("gameRequest", new GameRequest(game.name(), game.code(), game.description()));
		prepareForm(model, "Sửa game", BASE_URL + "/" + id);
		return FORM_VIEW;
	}

	@PostMapping("/{id}")
	public String update(
			@PathVariable Long id,
			@Valid @ModelAttribute("gameRequest") GameRequest request,
			BindingResult result,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (!result.hasErrors()) {
			try {
				gameCatalogService.update(id, request);
				redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật game");
				return "redirect:" + BASE_URL;
			} catch (ConflictException ex) {
				result.reject("game.conflict", ex.getMessage());
			}
		}
		prepareForm(model, "Sửa game", BASE_URL + "/" + id);
		return FORM_VIEW;
	}

	/** Ẩn (value=false) hoặc hiện lại (value=true) Game. */
	@PostMapping("/{id}/active")
	public String setActive(
			@PathVariable Long id,
			@RequestParam boolean value,
			RedirectAttributes redirectAttributes) {
		gameCatalogService.setActive(id, value);
		redirectAttributes.addFlashAttribute("successMessage", value ? "Đã hiện game" : "Đã ẩn game");
		return "redirect:" + BASE_URL;
	}

	/** Còn Series thì chỉ ẩn, không còn thì xóa hẳn (Service quyết định). */
	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		boolean deleted = gameCatalogService.removeOrHide(id);
		redirectAttributes.addFlashAttribute("successMessage",
				deleted ? "Đã xóa game" : "Game còn series liên quan nên đã được chuyển sang ẩn");
		return "redirect:" + BASE_URL;
	}

	private void prepareForm(Model model, String title, String formAction) {
		model.addAttribute("formAction", formAction);
		model.addAttribute("formTitle", title);
		addLayoutModel(model, title);
	}

	// GIẢ ĐỊNH: layout dùng 4 attribute này như DemoDashboardController của Phú
	private void addLayoutModel(Model model, String title) {
		String role = currentUser.hasRole(RoleName.ROLE_ADMIN) ? "ADMIN" : "MANAGER";
		model.addAttribute("dashboardRole", role);
		model.addAttribute("dashboardTitle", title);
		model.addAttribute("currentPath", BASE_URL);
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Danh mục", "url", BASE_URL),
				Map.of("label", "Game", "url", BASE_URL)));
	}
}