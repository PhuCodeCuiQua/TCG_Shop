package com.tcgshop.mapper;

import com.tcgshop.dto.response.CommissionSettingResponse;
import com.tcgshop.dto.response.ShopCommissionResponse;
import com.tcgshop.entity.CommissionSetting;
import com.tcgshop.entity.Shop;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommissionSettingMapper {

	@Mapping(target = "updatedByName", source = "updatedBy.fullName")
	CommissionSettingResponse toSettingResponse(CommissionSetting setting);

	@Mapping(target = "overrideRate", source = "shop.commissionRate")
	@Mapping(target = "effectiveRate",
			expression = "java(shop.getCommissionRate() == null ? defaultRate : shop.getCommissionRate())")
	@Mapping(target = "inheritingDefault", expression = "java(shop.getCommissionRate() == null)")
	ShopCommissionResponse toShopResponse(Shop shop, BigDecimal defaultRate);
}
