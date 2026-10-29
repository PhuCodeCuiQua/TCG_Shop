package com.tcgshop.dto.response;

public record SystemSettingResponse(String siteName, String logoUrl, String contactPhone, String contactEmail) {
    public static SystemSettingResponse defaults() {
        return new SystemSettingResponse("TCGSHOP", null, null, null);
    }
}
