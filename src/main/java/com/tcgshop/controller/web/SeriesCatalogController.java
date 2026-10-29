package com.tcgshop.controller.web;

import com.tcgshop.dto.request.SeriesRequest;
import com.tcgshop.dto.response.GameResponse;
import com.tcgshop.dto.response.SeriesResponse;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.GameCatalogService;
import com.tcgshop.service.SeriesCatalogService;
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

/** Quản lý Series (PA-10). Phân quyền giống GameCatalogController. */
@Controller
@RequestMapping("/manager/series")
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
@RequiredArgsConstructor
public class SeriesCatalogController {

	private static final int PAGE_SIZE = 10;
	private static final int GAME_DROPDOWN_SIZE = 200;
	private static final String BASE_URL = "/manager/series";
	private static final String LIST_VIEW = "manager/series/list";
	private static final String FORM_VIEW = "manager/series/form";

	private final SeriesCatalogService seriesCatalogService;
	private final GameCatalogService gameCatalogService;
	private final CurrentUser currentUser;

	@GetMapping
	public String list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Long gameId,
			@RequestParam(required = false) Boolean active,
			@RequestParam(defaultValue = "0") int page,
			Model model) {
		Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by("name").ascending());
		model.addAttribute("seriesPage", seriesCatalogService.search(keyword, gameId, active, pageable));
		model.addAttribute("games", activeGames());
		model.addAttribute("keyword", keyword);
		model.addAttribute("gameId", gameId);
		model.addAttribute("active", active);
		addLayoutModel(model, "Quản lý series");
		return LIST_VIEW;
	}

	@GetMapping("/new")
	public String newForm(@RequestParam(required = false) Long gameId, Model model) {
		model.addAttribute("seriesRequest", new SeriesRequest(gameId, null, null, null));
		prepareForm(model, "Thêm series", BASE_URL, true, null);
		return FORM_VIEW;
	}

	@PostMapping
	public String create(
			@Valid @ModelAttribute("seriesRequest") SeriesRequest request,
			BindingResult result,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (!result.hasErrors()) {
			try {
				seriesCatalogService.create(request);
				redirectAttributes.addFlashAttribute("successMessage", "Đã thêm series");
				return "redirect:" + BASE_URL;
			} catch (ConflictException ex) {
				// Chỉ bắt lỗi trùng để hiện lên form và giữ dữ liệu đã nhập
				result.reject("series.conflict", ex.getMessage());
			}
		}
		prepareForm(model, "Thêm series", BASE_URL, true, null);
		return FORM_VIEW;
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		SeriesResponse series = seriesCatalogService.getById(id);
		model.addAttribute("seriesRequest",
				new SeriesRequest(series.gameId(), series.name(), series.code(), series.releaseDate()));
		prepareForm(model, "Sửa series", BASE_URL + "/" + id, false, series.gameName());
		return FORM_VIEW;
	}

	@PostMapping("/{id}")
	public String update(
			@PathVariable Long id,
			@Valid @ModelAttribute("seriesRequest") SeriesRequest request,
			BindingResult result,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (!result.hasErrors()) {
			try {
				seriesCatalogService.update(id, request);
				redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật series");
				return "redirect:" + BASE_URL;
			} catch (ConflictException ex) {
				result.reject("series.conflict", ex.getMessage());
			}
		}
		// Khi lỗi, lấy lại tên game từ dữ liệu thật (không tin dữ liệu form)
		prepareForm(model, "Sửa series", BASE_URL + "/" + id, false,
				seriesCatalogService.getById(id).gameName());
		return FORM_VIEW;
	}

	@PostMapping("/{id}/active")
	public String setActive(
			@PathVariable Long id,
			@RequestParam boolean value,
			RedirectAttributes redirectAttributes) {
		seriesCatalogService.setActive(id, value);
		redirectAttributes.addFlashAttribute("successMessage", value ? "Đã hiện series" : "Đã ẩn series");
		return "redirect:" + BASE_URL;
	}

	/** Còn thẻ hoặc sản phẩm liên quan thì chỉ ẩn, không thì xóa hẳn (Service quyết định). */
	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		boolean deleted = seriesCatalogService.removeOrHide(id);
		redirectAttributes.addFlashAttribute("successMessage",
				deleted ? "Đã xóa series" : "Series còn thẻ hoặc sản phẩm liên quan nên đã được chuyển sang ẩn");
		return "redirect:" + BASE_URL;
	}

	private List<GameResponse> activeGames() {
		return gameCatalogService
				.search(null, true, PageRequest.of(0, GAME_DROPDOWN_SIZE, Sort.by("name").ascending()))
				.content();
	}

	private void prepareForm(Model model, String title, String formAction, boolean gameSelectable,
			String gameName) {
		model.addAttribute("formAction", formAction);
		model.addAttribute("formTitle", title);
		model.addAttribute("gameSelectable", gameSelectable); // sửa series không cho đổi game
		model.addAttribute("games", activeGames());
		model.addAttribute("seriesGameName", gameName);
		addLayoutModel(model, title);
	}

	// GIẢ ĐỊNH: layout dùng 4 attribute này như DemoDashboardController của Phú
	private void addLayoutModel(Model model, String title) {
		String role = currentUser.hasRole(RoleName.ROLE_ADMIN) ? "ADMIN" : "MANAGER";
		model.addAttribute("dashboardRole", role);
		model.addAttribute("dashboardTitle", title);
		model.addAttribute("currentPath", BASE_URL);
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Danh mục", "url", "/manager/games"),
				Map.of("label", "Series", "url", BASE_URL)));
	}
}