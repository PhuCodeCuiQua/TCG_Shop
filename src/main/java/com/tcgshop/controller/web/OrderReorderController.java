package com.tcgshop.controller.web;

import com.tcgshop.service.OrderReorderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class OrderReorderController {
    private final OrderReorderService service;

    @PostMapping("/orders/{id}/reorder")
    public String reorder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        var result = service.reorder(id);
        if (result.addedItemCount() > 0) {
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm " + result.addedItemCount() + " sản phẩm vào giỏ hàng theo giá hiện tại");
        }
        if (!result.skippedItems().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã bỏ qua: " + String.join("; ", result.skippedItems()));
        } else if (result.addedItemCount() == 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đơn hàng không có sản phẩm có thể mua lại");
        }
        return "redirect:/cart";
    }
}
