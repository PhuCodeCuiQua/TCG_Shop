package com.tcgshop.mapper;

import com.tcgshop.dto.request.SystemSettingRequest;
import com.tcgshop.dto.response.SystemSettingResponse;
import com.tcgshop.entity.SystemSetting;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SystemSettingMapper {
    SystemSettingResponse toResponse(SystemSetting entity);
    @Mapping(target = "logo", ignore = true)
    SystemSettingRequest toRequest(SystemSettingResponse response);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "siteName", expression = "java(request.getSiteName().trim())")
    @Mapping(target = "contactPhone", expression = "java(request.getContactPhone().trim())")
    @Mapping(target = "contactEmail", expression = "java(request.getContactEmail().trim())")
    void update(SystemSettingRequest request, @MappingTarget SystemSetting entity);
}
