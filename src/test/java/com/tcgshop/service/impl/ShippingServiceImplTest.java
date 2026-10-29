package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.dto.request.ShippingCarrierRequest;
import com.tcgshop.dto.response.ShippingCarrierResponse;
import com.tcgshop.entity.ShippingCarrier;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.BusinessException;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.mapper.ShippingCarrierMapper;
import com.tcgshop.repository.ShippingCarrierRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

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

	@Test
	void returnsOnlyActiveCarriersForCheckout() {
		ShippingCarrier carrier = carrier(true, "25000", null);
		when(carrierRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(carrier));
		when(carrierMapper.toResponse(carrier))
				.thenReturn(new ShippingCarrierResponse(1L, "Carrier", "CARRIER", null, new BigDecimal("25000"), null));

		assertEquals(1, service.getActiveCarriers().size());
		verify(carrierRepository).findAllByActiveTrueOrderByNameAsc();
	}

	@Test
	void create_duplicateName_throwsConflictWithoutSaving() {
		ShippingCarrierRequest request = validRequest();
		when(carrierRepository.existsByNameIgnoreCase("Carrier")).thenReturn(true);

		assertThrows(ConflictException.class, () -> service.create(request));

		verify(carrierRepository, never()).save(any());
	}

	@Test
	void create_validRequest_savesNormalizedCarrier() {
		ShippingCarrierRequest request = validRequest();
		when(carrierRepository.save(any(ShippingCarrier.class))).thenAnswer(invocation -> {
			ShippingCarrier saved = invocation.getArgument(0);
			saved.setId(2L);
			return saved;
		});
		when(carrierRepository.countOrdersUsingCarrier(2L)).thenReturn(0L);
		when(carrierMapper.toAdminResponse(any(ShippingCarrier.class), anyLong()))
				.thenReturn(new com.tcgshop.dto.response.AdminShippingCarrierResponse(
						2L, "Carrier", "CARRIER", null, new BigDecimal("25000"), new BigDecimal("200000"), true, 0));

		service.create(request);

		org.mockito.ArgumentCaptor<ShippingCarrier> captor =
				org.mockito.ArgumentCaptor.forClass(ShippingCarrier.class);
		verify(carrierRepository).save(captor.capture());
		assertEquals("Carrier", captor.getValue().getName());
		assertEquals("CARRIER", captor.getValue().getCode());
		assertEquals(new BigDecimal("25000"), captor.getValue().getBaseFee());
		assertTrue(captor.getValue().isActive());
	}

	@Test
	void create_negativeFee_throwsBadRequestWithoutSaving() {
		ShippingCarrierRequest request = validRequest();
		request.setBaseFee(new BigDecimal("-1"));

		assertThrows(BadRequestException.class, () -> service.create(request));

		verify(carrierRepository, never()).save(any());
	}

	@Test
	void create_logoUploadIsBlockedUntilPublicIdCanBeStored() {
		ShippingCarrierRequest request = validRequest();
		request.setLogo(new MockMultipartFile("logo", "logo.png", "image/png", new byte[] {1}));

		assertThrows(BusinessException.class, () -> service.create(request));

		verify(carrierRepository, never()).save(any());
	}

	@Test
	void removeOrHide_carrierHasOrders_disablesInsteadOfDeleting() {
		ShippingCarrier carrier = carrier(true, "25000", null);
		carrier.setId(1L);
		when(carrierRepository.findById(1L)).thenReturn(Optional.of(carrier));
		when(carrierRepository.countOrdersUsingCarrier(1L)).thenReturn(2L);

		assertFalse(service.removeOrHide(1L));

		assertFalse(carrier.isActive());
		verify(carrierRepository, never()).delete(any());
	}

	@Test
	void removeOrHide_carrierUnusedAndWithoutLogo_deletesPermanently() {
		ShippingCarrier carrier = carrier(true, "25000", null);
		carrier.setId(1L);
		when(carrierRepository.findById(1L)).thenReturn(Optional.of(carrier));
		when(carrierRepository.countOrdersUsingCarrier(1L)).thenReturn(0L);

		assertTrue(service.removeOrHide(1L));

		verify(carrierRepository).delete(carrier);
	}

	@Test
	void removeOrHide_carrierWithLogo_hidesToAvoidOrphaningCloudinaryAsset() {
		ShippingCarrier carrier = carrier(true, "25000", null);
		carrier.setId(1L);
		carrier.setLogoUrl("https://cloudinary.example/logo.png");
		when(carrierRepository.findById(1L)).thenReturn(Optional.of(carrier));
		when(carrierRepository.countOrdersUsingCarrier(1L)).thenReturn(0L);

		assertFalse(service.removeOrHide(1L));

		assertFalse(carrier.isActive());
		verify(carrierRepository, never()).delete(any());
	}

	@Test
	void update_missingCarrier_throwsNotFound() {
		when(carrierRepository.findById(1L)).thenReturn(Optional.empty());

		assertThrows(com.tcgshop.exception.ResourceNotFoundException.class,
				() -> service.update(1L, validRequest()));
	}

	private ShippingCarrierRequest validRequest() {
		ShippingCarrierRequest request = new ShippingCarrierRequest();
		request.setName("Carrier");
		request.setCode("carrier");
		request.setBaseFee(new BigDecimal("25000"));
		request.setFreeShipThreshold(new BigDecimal("200000"));
		request.setActive(true);
		return request;
	}

	private ShippingCarrier carrier(boolean active, String baseFee, String threshold) {
		ShippingCarrier carrier = new ShippingCarrier();
		carrier.setActive(active);
		carrier.setBaseFee(new BigDecimal(baseFee));
		carrier.setFreeShipThreshold(threshold == null ? null : new BigDecimal(threshold));
		return carrier;
	}
}
