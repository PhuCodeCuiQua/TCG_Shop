package com.tcgshop.service;

import com.tcgshop.dto.request.SystemSettingRequest;
import com.tcgshop.dto.response.SystemSettingResponse;

public interface SystemSettingService {
    SystemSettingResponse getSettings();
    void save(SystemSettingRequest request);
}
