package com.tcgshop.mapper;

import org.mapstruct.MapperConfig;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;

@MapperConfig(
		componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.WARN,
		nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface CommonMapperConfig {
}
