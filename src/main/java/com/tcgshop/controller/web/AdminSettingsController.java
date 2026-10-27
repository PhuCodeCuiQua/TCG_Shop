package com.tcgshop.controller.web;

import com.tcgshop.dto.request.SystemSettingRequest;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.CloudinaryUploadException;
import com.tcgshop.mapper.SystemSettingMapper;
import com.tcgshop.service.SystemSettingService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/settings")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminSettingsController {
    private final SystemSettingService service;
    private final SystemSettingMapper mapper;

    @GetMapping
    public String settings(Model model) {
        model.addAttribute("settingsRequest", mapper.toRequest(service.getSettings()));
        return form(model);
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("settingsRequest") SystemSettingRequest request,
            BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) return form(model);
        try {
            service.save(request);
        } catch (BadRequestException | CloudinaryUploadException exception) {
            result.rejectValue("logo", "settings.logo", exception.getMessage());
            return form(model);
        }
        redirectAttributes.addFlashAttribute("successMessage", "Đã lưu cấu hình hệ thống thành công");
        return "redirect:/admin/settings";
    }

    private String form(Model model) {
        model.addAttribute("dashboardRole", "ADMIN");
        model.addAttribute("dashboardTitle", "Cấu hình hệ thống");
        model.addAttribute("currentPath", "/admin/settings");
        model.addAttribute("breadcrumbItems", List.of(Map.of("label", "Dashboard", "url", "/admin/dashboard"),
                Map.of("label", "Cấu hình hệ thống", "url", "/admin/settings")));
        return "admin/settings/index";
    }
}
