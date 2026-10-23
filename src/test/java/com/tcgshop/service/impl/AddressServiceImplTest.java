package com.tcgshop.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.config.AddressProperties;
import com.tcgshop.dto.request.AddressRequest;
import com.tcgshop.dto.response.AddressResponse;
import com.tcgshop.entity.Address;
import com.tcgshop.entity.User;
import com.tcgshop.exception.BusinessException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.AddressMapper;
import com.tcgshop.repository.AddressRepository;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.security.CurrentUser;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {
	private static final long USER_ID = 44L;
	@Mock private AddressRepository addressRepository;
	@Mock private UserRepository userRepository;
	@Mock private CurrentUser currentUser;
	@Mock private AddressMapper addressMapper;
	private AddressServiceImpl addressService;
	private User user;

	@BeforeEach
	void setUp() {
		addressService = new AddressServiceImpl(addressRepository, userRepository, currentUser,
				addressMapper, new AddressProperties(10));
		user = new User();
		user.setId(USER_ID);
		when(currentUser.getId()).thenReturn(USER_ID);
		when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
		when(addressMapper.toResponse(any(Address.class))).thenAnswer(invocation -> response(invocation.getArgument(0)));
	}

	@Test
	void firstAddressBecomesDefaultAndLocksUserBeforeCountingOrSaving() {
		when(addressRepository.countByUserId(USER_ID)).thenReturn(0L);
		when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

		AddressResponse response = addressService.create(request("Nguyễn Văn An", false));

		assertTrue(response.isDefault());
		InOrder order = inOrder(userRepository, addressRepository);
		order.verify(userRepository).findByIdForUpdate(USER_ID);
		order.verify(addressRepository).countByUserId(USER_ID);
		order.verify(addressRepository).save(any(Address.class));
	}

	@Test
	void addingSecondNonDefaultAddressKeepsCurrentDefault() {
		Address currentDefault = address(1L, true);
		when(addressRepository.countByUserId(USER_ID)).thenReturn(1L);
		when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

		AddressResponse created = addressService.create(request("Địa chỉ thứ hai", false));

		assertFalse(created.isDefault());
		assertTrue(currentDefault.isDefault());
		verify(addressRepository, never()).clearDefaultByUserId(USER_ID);
		InOrder order = inOrder(userRepository, addressRepository);
		order.verify(userRepository).findByIdForUpdate(USER_ID);
		order.verify(addressRepository).countByUserId(USER_ID);
		order.verify(addressRepository).save(any(Address.class));
	}

	@Test
	void creatingWithMakeDefaultClearsPreviousDefault() {
		when(addressRepository.countByUserId(USER_ID)).thenReturn(1L);
		when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

		assertTrue(addressService.create(request("Địa chỉ mới", true)).isDefault());

		InOrder order = inOrder(userRepository, addressRepository);
		order.verify(userRepository).findByIdForUpdate(USER_ID);
		order.verify(addressRepository).countByUserId(USER_ID);
		order.verify(addressRepository).clearDefaultByUserId(USER_ID);
		order.verify(addressRepository).save(any(Address.class));
	}

	@Test
	void createRejectsAddressLimitAfterLockingUser() {
		addressService = new AddressServiceImpl(addressRepository, userRepository, currentUser, addressMapper,
				new AddressProperties(1));
		when(addressRepository.countByUserId(USER_ID)).thenReturn(1L);

		BusinessException exception = assertThrows(BusinessException.class,
				() -> addressService.create(request("Địa chỉ", false)));

		assertEquals("Bạn chỉ có thể lưu tối đa 1 địa chỉ", exception.getMessage());
		InOrder order = inOrder(userRepository, addressRepository);
		order.verify(userRepository).findByIdForUpdate(USER_ID);
		order.verify(addressRepository).countByUserId(USER_ID);
		verify(addressRepository, never()).save(any(Address.class));
	}

	@Test
	void updateDefaultWithMakeDefaultFalsePreservesDefaultAndNormalizesFields() {
		Address existing = address(7L, true);
		when(addressRepository.findByIdAndUserId(7L, USER_ID)).thenReturn(Optional.of(existing));
		when(addressRepository.save(existing)).thenReturn(existing);

		AddressRequest request = request("  Nguyễn   Văn An  ", false);
		request.setPhoneNumber("0912345678");
		request.setProvince("  Thành   phố Hồ Chí Minh ");
		request.setDetailAddress("  12   Đường   A ");
		AddressResponse updated = addressService.update(7L, request);

		assertTrue(updated.isDefault());
		assertEquals("Nguyễn Văn An", existing.getReceiverName());
		assertEquals("Thành phố Hồ Chí Minh", existing.getProvince());
		assertEquals("12 Đường A", existing.getDetailAddress());
		InOrder order = inOrder(userRepository, addressRepository);
		order.verify(userRepository).findByIdForUpdate(USER_ID);
		order.verify(addressRepository).findByIdAndUserId(7L, USER_ID);
		order.verify(addressRepository).save(existing);
	}

	@Test
	void updateSetDefaultAndDeleteReturnNotFoundForAnotherUsersAddress() {
		when(addressRepository.findByIdAndUserId(8L, USER_ID)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> addressService.update(8L, request("Receiver", false)));
		assertThrows(ResourceNotFoundException.class, () -> addressService.setDefault(8L));
		assertThrows(ResourceNotFoundException.class, () -> addressService.delete(8L));
		verify(userRepository, org.mockito.Mockito.times(3)).findByIdForUpdate(USER_ID);
		verify(addressRepository, never()).save(any(Address.class));
		verify(addressRepository, never()).delete(any(Address.class));
	}

	@Test
	void setDefaultLocksBeforeOwnershipLookupAndDefaultChanges() {
		Address address = address(4L, false);
		when(addressRepository.findByIdAndUserId(4L, USER_ID)).thenReturn(Optional.of(address));
		when(addressRepository.save(address)).thenReturn(address);

		assertTrue(addressService.setDefault(4L).isDefault());

		InOrder order = inOrder(userRepository, addressRepository);
		order.verify(userRepository).findByIdForUpdate(USER_ID);
		order.verify(addressRepository).findByIdAndUserId(4L, USER_ID);
		order.verify(addressRepository).clearDefaultByUserId(USER_ID);
		order.verify(addressRepository).findByIdAndUserId(4L, USER_ID);
		order.verify(addressRepository).save(address);
	}

	@Test
	void deletingDefaultPromotesNewestRemainingAddress() {
		Address oldDefault = address(3L, true);
		Address newest = address(9L, false);
		when(addressRepository.findByIdAndUserId(3L, USER_ID)).thenReturn(Optional.of(oldDefault));
		when(addressRepository.findFirstByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(Optional.of(newest));
		when(addressRepository.save(newest)).thenReturn(newest);

		addressService.delete(3L);

		assertTrue(newest.isDefault());
		InOrder order = inOrder(userRepository, addressRepository);
		order.verify(userRepository).findByIdForUpdate(USER_ID);
		order.verify(addressRepository).findByIdAndUserId(3L, USER_ID);
		order.verify(addressRepository).delete(oldDefault);
		order.verify(addressRepository).flush();
		order.verify(addressRepository).findFirstByUserIdOrderByCreatedAtDesc(USER_ID);
		order.verify(addressRepository).save(newest);
	}

	@Test
	void deletingOnlyAddressLeavesNoPromotionCandidate() {
		Address only = address(1L, true);
		when(addressRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(only));
		when(addressRepository.findFirstByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(Optional.empty());

		addressService.delete(1L);

		verify(addressRepository).delete(only);
		verify(addressRepository).findFirstByUserIdOrderByCreatedAtDesc(USER_ID);
		verify(addressRepository, never()).save(any(Address.class));
	}

	@Test
	void everyWriteLocksUserBeforeAddressReadsOrWrites() {
		Address defaultAddress = address(1L, true);
		when(addressRepository.countByUserId(USER_ID)).thenReturn(1L);
		when(addressRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(defaultAddress));
		when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

		addressService.create(request("Second address", false));
		addressService.update(1L, request("Updated address", false));
		addressService.setDefault(1L);
		addressService.delete(1L);

		InOrder order = inOrder(userRepository, addressRepository);
		order.verify(userRepository).findByIdForUpdate(USER_ID);
		order.verify(addressRepository).countByUserId(USER_ID);
		order.verify(addressRepository).save(any(Address.class));
		order.verify(userRepository).findByIdForUpdate(USER_ID);
		order.verify(addressRepository).findByIdAndUserId(1L, USER_ID);
		order.verify(addressRepository).save(any(Address.class));
		order.verify(userRepository).findByIdForUpdate(USER_ID);
		order.verify(addressRepository).findByIdAndUserId(1L, USER_ID);
		order.verify(addressRepository).clearDefaultByUserId(USER_ID);
		order.verify(addressRepository).findByIdAndUserId(1L, USER_ID);
		order.verify(addressRepository).save(any(Address.class));
		order.verify(userRepository).findByIdForUpdate(USER_ID);
		order.verify(addressRepository).findByIdAndUserId(1L, USER_ID);
		order.verify(addressRepository).delete(defaultAddress);
	}

	private AddressRequest request(String receiverName, boolean makeDefault) {
		AddressRequest request = new AddressRequest();
		request.setReceiverName(receiverName);
		request.setPhoneNumber("0912345678");
		request.setProvince("Hồ Chí Minh");
		request.setDistrict("Quận 1");
		request.setWard("Phường 1");
		request.setDetailAddress("12 Đường A");
		request.setMakeDefault(makeDefault);
		return request;
	}

	private Address address(Long id, boolean isDefault) {
		Address address = new Address();
		address.setId(id);
		address.setUser(user);
		address.setReceiverName("Nguyễn Văn An");
		address.setPhoneNumber("0912345678");
		address.setProvince("Hồ Chí Minh");
		address.setDistrict("Quận 1");
		address.setWard("Phường 1");
		address.setDetailAddress("12 Đường A");
		address.setDefault(isDefault);
		return address;
	}

	private AddressResponse response(Address address) {
		return new AddressResponse(address.getId(), address.getReceiverName(), address.getPhoneNumber(),
				address.getProvince(), address.getDistrict(), address.getWard(), address.getDetailAddress(),
				address.getDetailAddress() + ", " + address.getWard() + ", " + address.getDistrict() + ", "
						+ address.getProvince(), address.isDefault(), address.getCreatedAt());
	}
}
