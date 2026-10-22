package com.tcgshop.mapper;

import com.tcgshop.dto.response.NotificationResponse;
import com.tcgshop.entity.Notification;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

	@Mapping(target = "read", source = "read")
	@Mapping(target = "typeDisplayName",
			expression = "java(notification.getType() == null ? null : notification.getType().getDisplayName())")
	NotificationResponse toResponse(Notification notification);

	List<NotificationResponse> toResponseList(List<Notification> notifications);
}