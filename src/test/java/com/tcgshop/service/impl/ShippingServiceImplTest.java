package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.tcgshop.entity.ShippingCarrier;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.mapper.ShippingCarrierMapper;
import com.tcgshop.repository.ShippingCarrierRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShippingServiceImplTest {
	@Mock private ShippingCarrierRepository carrierRepository;
	@Mock private ShippingCarrierMapper carrierMapper;

	private ShippingServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new ShippingServiceImpl(carrierRepository, carrierMapper);
	}

	@Test
	void returnsZeroWhenSubtotalMeetsFreeShippingThreshold() {
		ShippingCarrier carrier = carrier(true, "25000", "200000");
		when(carrierRepository.findById(1L)).thenReturn(Optional.of(carrier));

		assertEquals(BigDecimal.ZERO, service.calculateFee(1L, new BigDecimal("200000")));
	}

	@Test
	void returnsBaseFeeWhenSubtotalIsBelowFreeShippingThreshold() {
		ShippingCarrier carrier = carrier(true, "25000", "200000");
		when(carrierRepository.findById(1L)).thenReturn(Optional.of(carrier));

		assertEquals(new BigDecimal("25000"), service.calculateFee(1L, new BigDecimal("199999")));
	}

	@Test
	void rejectsInactiveCarrier() {
		ShippingCarrier carrier = carrier(false, "25000", null);
		when(carrierRepository.findById(1L)).thenReturn(Optional.of(carrier));

		assertThrows(BadRequestException.class,
				() -> service.calculateFee(1L, new BigDecimal("300000")));
	}

	private ShippingCarrier carrier(boolean active, String baseFee, String threshold) {
		ShippingCarrier carrier = new ShippingCarrier();
		carrier.setActive(active);
		carrier.setBaseFee(new BigDecimal(baseFee));
		carrier.setFreeShipThreshold(threshold == null ? null : new BigDecimal(threshold));
		return carrier;
	}
}
