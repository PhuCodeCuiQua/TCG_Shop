package com.tcgshop.controller.web;

import com.tcgshop.dto.response.SystemSettingResponse;
import com.tcgshop.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "com.tcgshop.controller.web")
@RequiredArgsConstructor
public class GlobalSettingsAdvice {
    private final ObjectProvider<SystemSettingService> serviceProvider;

    @ModelAttribute("globalSettings")
    public SystemSettingResponse globalSettings() {
        var service = serviceProvider.getIfAvailable();
        return service == null ? SystemSettingResponse.defaults() : service.getSettings();
    }
}
