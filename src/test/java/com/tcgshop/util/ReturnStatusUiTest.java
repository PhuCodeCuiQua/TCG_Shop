package com.tcgshop.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.tcgshop.entity.enums.ReturnStatus;
import org.junit.jupiter.api.Test;

class ReturnStatusUiTest {
	@Test
	void everyReturnStatusHasLabelAndColor() {
		for (ReturnStatus status : ReturnStatus.values()) {
			assertNotNull(ReturnStatusUi.label(status), status.name());
			assertNotNull(ReturnStatusUi.color(status), status.name());
		}
	}
}
