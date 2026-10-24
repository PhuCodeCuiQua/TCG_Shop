package com.tcgshop.mapper;

import com.tcgshop.dto.response.VendorRevenueEntryResponse;
import com.tcgshop.repository.VendorRevenueRepository.VendorRevenueOrderRow;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VendorRevenueMapper {
	VendorRevenueEntryResponse toResponse(VendorRevenueOrderRow row);
}
