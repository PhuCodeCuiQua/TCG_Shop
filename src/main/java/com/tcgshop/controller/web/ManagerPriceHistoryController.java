package com.tcgshop.controller.web;

import com.tcgshop.dto.request.PriceHistoryEditRequest;
import com.tcgshop.dto.request.PriceHistoryRequest;
import com.tcgshop.dto.response.PriceHistoryResponse;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.PriceHistoryService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
@RequestMapping("/manager/price-history")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
@RequiredArgsConstructor
public class ManagerPriceHistoryController {

	private static final int PAGE_SIZE = 20;
	private static final String BASE_URL = "/manager/price-history";
	private static final String LIST_VIEW = "manager/price-history/list";
	private static final String EDIT_VIEW = "manager/price-history/edit";

	private final PriceHistoryService priceHistoryService;
	private final CurrentUser currentUser;

	@GetMapping
	public String list(
			@RequestParam(required = false) Long cardId,
			@RequestParam(required = false) LocalDate startDate,
			@RequestParam(required = false) LocalDate endDate,
			@RequestParam(defaultValue = "0") int page,
			Model model) {
		loadListModel(model, cardId, startDate, endDate, page);
		return LIST_VIEW;
	}

	@PostMapping
	public String save(
			@Valid @ModelAttribute("priceHistoryRequest") PriceHistoryRequest request,
			BindingResult result,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (!result.hasErrors()) {
			try {
				priceHistoryService.save(request);
				redirectAttributes.addFlashAttribute("successMessage", "Đã lưu lịch sử giá và cập nhật giá thị trường hiện tại");
				return redirectToList(request.getCardId(), request.getRecordedDate(), request.getRecordedDate());
			} catch (ConflictException exception) {
				result.reject("priceHistory.conflict", exception.getMessage());
			}
		}
		loadListModel(model, request.getCardId(), request.getRecordedDate(), request.getRecordedDate(), 0);
		return LIST_VIEW;
	}

	@GetMapping("/{historyId}/edit")
	public String editForm(@PathVariable Long historyId, Model model) {
		PriceHistoryResponse history = priceHistoryService.getById(historyId);
		prepareEditModel(model, history, editRequest(history));
		return EDIT_VIEW;
	}

	@PostMapping("/{historyId}/edit")
	public String update(
			@PathVariable Long historyId,
			@Valid @ModelAttribute("priceHistoryEditRequest") PriceHistoryEditRequest request,
			BindingResult result,
			Model model,
			RedirectAttributes redirectAttributes) {
		PriceHistoryResponse history = priceHistoryService.getById(historyId);
		if (!result.hasErrors()) {
			priceHistoryService.update(historyId, request);
			redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật lịch sử giá");
			return redirectToList(history.cardId(), history.recordedDate(), history.recordedDate());
		}
		prepareEditModel(model, history, request);
		return EDIT_VIEW;
	}

	@PostMapping("/{historyId}/delete")
	public String delete(@PathVariable Long historyId, RedirectAttributes redirectAttributes) {
		PriceHistoryResponse history = priceHistoryService.getById(historyId);
		priceHistoryService.delete(historyId);
		redirectAttributes.addFlashAttribute("successMessage", "Đã xóa bản ghi lịch sử giá");
		return redirectToList(history.cardId(), null, null);
	}

	private void loadListModel(
			Model model, Long cardId, LocalDate startDate, LocalDate endDate, int page) {
		model.addAttribute("historyPage", priceHistoryService.search(
				cardId, startDate, endDate,
				PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "recordedDate"))));
		model.addAttribute("cardOptions", priceHistoryService.getActiveCards());
		model.addAttribute("selectedCardId", cardId);
		model.addAttribute("startDate", startDate);
		model.addAttribute("endDate", endDate);
		if (!model.containsAttribute("priceHistoryRequest")) {
			PriceHistoryRequest request = new PriceHistoryRequest();
			request.setCardId(cardId);
			request.setRecordedDate(LocalDate.now());
			model.addAttribute("priceHistoryRequest", request);
		}
		addLayoutModel(model, "Quản lý lịch sử giá");
	}

	private void prepareEditModel(Model model, PriceHistoryResponse history, PriceHistoryEditRequest request) {
		model.addAttribute("history", history);
		model.addAttribute("priceHistoryEditRequest", request);
		addLayoutModel(model, "Sửa lịch sử giá");
	}

	private PriceHistoryEditRequest editRequest(PriceHistoryResponse history) {
		PriceHistoryEditRequest request = new PriceHistoryEditRequest();
		request.setMarketPrice(history.marketPrice());
		request.setAvgPrice(history.avgPrice());
		return request;
	}

	private String redirectToList(Long cardId, LocalDate startDate, LocalDate endDate) {
		StringBuilder url = new StringBuilder("redirect:").append(BASE_URL);
		if (cardId != null) {
			url.append("?cardId=").append(cardId);
			if (startDate != null) {
				url.append("&startDate=").append(startDate);
			}
			if (endDate != null) {
				url.append("&endDate=").append(endDate);
			}
		}
		return url.toString();
	}

	private void addLayoutModel(Model model, String title) {
		String role = currentUser.hasRole(RoleName.ROLE_ADMIN) ? "ADMIN" : "MANAGER";
		model.addAttribute("dashboardRole", role);
		model.addAttribute("dashboardTitle", title);
		model.addAttribute("currentPath", BASE_URL);
		model.addAttribute("breadcrumbItems", List.of(
				Map.of("label", "Danh mục", "url", BASE_URL),
				Map.of("label", "Lịch sử giá", "url", BASE_URL)));
	}
}
