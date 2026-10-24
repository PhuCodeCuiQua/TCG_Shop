package com.tcgshop.mapper;

import com.tcgshop.dto.response.VendorReturnResponse;
import com.tcgshop.repository.VendorReturnRepository.VendorReturnRow;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VendorReturnMapper {
	VendorReturnResponse toResponse(VendorReturnRow row);
}
