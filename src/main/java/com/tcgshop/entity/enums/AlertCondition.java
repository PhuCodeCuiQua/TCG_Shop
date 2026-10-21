package com.tcgshop.entity.enums;

import lombok.Getter;

@Getter
public enum AlertCondition {

	BELOW("Thấp hơn"),
	ABOVE("Cao hơn");

	private final String displayName;

	AlertCondition(String displayName) {
		this.displayName = displayName;
	}
}
