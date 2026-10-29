package com.tcgshop.mapper;

import com.tcgshop.dto.response.ReturnComplaintDetailResponse;
import com.tcgshop.dto.response.ReturnComplaintDetailResponse.TimelineItem;
import com.tcgshop.dto.response.ReturnComplaintResponse;
import com.tcgshop.entity.ReturnRequest;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReturnComplaintMapper {

	@Mapping(target = "orderId", source = "order.id")
	@Mapping(target = "orderCode", source = "order.orderCode")
	@Mapping(target = "buyerName", source = "user.fullName")
	@Mapping(target = "shopName", source = "order.shop.name")
	@Mapping(target = "totalAmount", source = "order.totalAmount")
	@Mapping(target = "orderStatus", source = "order.status")
	@Mapping(target = "returnStatus", source = "status")
	@Mapping(target = "requestedAt", source = "createdAt")
	ReturnComplaintResponse toResponse(ReturnRequest request);

	@Mapping(target = "orderId", source = "request.order.id")
	@Mapping(target = "orderCode", source = "request.order.orderCode")
	@Mapping(target = "buyerName", source = "request.user.fullName")
	@Mapping(target = "shopName", source = "request.order.shop.name")
	@Mapping(target = "totalAmount", source = "request.order.totalAmount")
	@Mapping(target = "reason", source = "request.reason")
	@Mapping(target = "proofImageUrl", source = "request.proofImageUrl")
	@Mapping(target = "proofVideoUrl", source = "request.proofVideoUrl")
	@Mapping(target = "vendorNote", source = "request.vendorNote")
	@Mapping(target = "adminNote", source = "request.adminNote")
	@Mapping(target = "orderStatus", source = "request.order.status")
	@Mapping(target = "returnStatus", source = "request.status")
	@Mapping(target = "requestedAt", source = "request.createdAt")
	@Mapping(target = "decidedAt", source = "request.decidedAt")
	@Mapping(target = "timeline", source = "timeline")
	ReturnComplaintDetailResponse toDetail(ReturnRequest request, List<TimelineItem> timeline);
}