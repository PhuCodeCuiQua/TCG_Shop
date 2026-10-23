package com.tcgshop.service.impl;

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
import com.tcgshop.service.AddressService;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AddressServiceImpl implements AddressService {

	private final AddressRepository addressRepository;
	private final UserRepository userRepository;
	private final CurrentUser currentUser;
	private final AddressMapper addressMapper;
	private final AddressProperties addressProperties;

	public AddressServiceImpl(AddressRepository addressRepository, UserRepository userRepository,
			CurrentUser currentUser, AddressMapper addressMapper, AddressProperties addressProperties) {
		this.addressRepository = addressRepository;
		this.userRepository = userRepository;
		this.currentUser = currentUser;
		this.addressMapper = addressMapper;
		this.addressProperties = addressProperties;
	}

	@Override
	public List<AddressResponse> list() {
		Long userId = currentUser.getId();
		lockCurrentUser();
		ensureSingleDefaultAddress(userId);
		return addressRepository.findByUserIdOrderByDefaultDescCreatedAtDesc(userId).stream()
				.map(addressMapper::toResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public AddressResponse get(Long id) {
		return addressMapper.toResponse(findOwnedAddress(id, currentUser.getId()));
	}

	@Override
	@Transactional(readOnly = true)
	public AddressResponse getForCheckout(Long id) {
		// Dùng cùng kiểm tra sở hữu để checkout chỉ lấy địa chỉ của người đang đăng nhập làm snapshot.
		return addressMapper.toResponse(findOwnedAddress(id, currentUser.getId()));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<AddressResponse> findDefault() {
		return addressRepository.findByUserIdOrderByDefaultDescCreatedAtDesc(currentUser.getId()).stream()
				.filter(Address::isDefault).findFirst().map(addressMapper::toResponse);
	}

	@Override
	public AddressResponse create(AddressRequest request) {
		User user = lockCurrentUser();
		Long userId = user.getId();
		ensureSingleDefaultAddress(userId);
		long count = addressRepository.countByUserId(userId);
		if (count >= addressProperties.maxPerUser()) {
			throw new BusinessException("Bạn chỉ có thể lưu tối đa " + addressProperties.maxPerUser() + " địa chỉ");
		}

		boolean makeDefault = count == 0 || request.isMakeDefault();
		if (request.isMakeDefault() && count > 0) {
			addressRepository.clearDefaultByUserId(userId);
		}

		Address address = new Address();
		address.setUser(user);
		applyRequest(address, request);
		address.setDefault(makeDefault);
		return addressMapper.toResponse(addressRepository.save(address));
	}

	@Override
	public AddressResponse update(Long id, AddressRequest request) {
		User user = lockCurrentUser();
		Long userId = user.getId();
		ensureSingleDefaultAddress(userId);
		Address address = findOwnedAddress(id, userId);
		boolean wasDefault = address.isDefault();

		if (request.isMakeDefault() && !wasDefault) {
			addressRepository.clearDefaultByUserId(userId);
			address = findOwnedAddress(id, userId);
			address.setDefault(true);
		}
		applyRequest(address, request);
		// Bỏ chọn địa chỉ mặc định trong form không được làm mất địa chỉ mặc định hiện tại.
		if (wasDefault) {
			address.setDefault(true);
		}
		return addressMapper.toResponse(addressRepository.save(address));
	}

	@Override
	public AddressResponse setDefault(Long id) {
		User user = lockCurrentUser();
		Long userId = user.getId();
		ensureSingleDefaultAddress(userId);
		findOwnedAddress(id, userId);
		addressRepository.clearDefaultByUserId(userId);
		Address address = findOwnedAddress(id, userId);
		address.setDefault(true);
		return addressMapper.toResponse(addressRepository.save(address));
	}

	@Override
	public void delete(Long id) {
		User user = lockCurrentUser();
		Long userId = user.getId();
		ensureSingleDefaultAddress(userId);
		Address address = findOwnedAddress(id, userId);
		boolean wasDefault = address.isDefault();
		// Địa chỉ có thể xóa cứng vì đơn hàng lưu thông tin giao nhận dưới dạng snapshot.
		addressRepository.delete(address);
		addressRepository.flush();

		if (wasDefault) {
			addressRepository.findFirstByUserIdOrderByCreatedAtDesc(userId).ifPresent(newest -> {
				newest.setDefault(true);
				addressRepository.save(newest);
			});
		}
	}

	private User lockCurrentUser() {
		Long userId = currentUser.getId();
		// Khóa dòng user để tuần tự hóa cả lúc chưa có địa chỉ; khi đó chưa tồn tại dòng address nào để khóa.
		return userRepository.findByIdForUpdate(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Người dùng", userId));
	}

	private void ensureSingleDefaultAddress(Long userId) {
		List<Address> addresses = addressRepository.findByUserIdOrderByDefaultDescCreatedAtDesc(userId);
		long defaultCount = addresses.stream().filter(Address::isDefault).count();
		if (addresses.isEmpty() || defaultCount == 1) {
			return;
		}

		// Dữ liệu cũ có thể thiếu hoặc trùng địa chỉ mặc định; chọn địa chỉ mới nhất để sửa về đúng một mặc định.
		addressRepository.clearDefaultByUserId(userId);
		addressRepository.findFirstByUserIdOrderByCreatedAtDesc(userId).ifPresent(newest -> {
			newest.setDefault(true);
			addressRepository.save(newest);
		});
	}

	private Address findOwnedAddress(Long id, Long userId) {
		// Trả 404 cho cả địa chỉ không tồn tại lẫn không thuộc user để không tiết lộ sự tồn tại của địa chỉ khác.
		return addressRepository.findByIdAndUserId(id, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Địa chỉ", id));
	}

	private void applyRequest(Address address, AddressRequest request) {
		address.setReceiverName(normalize(request.getReceiverName()));
		address.setPhoneNumber(normalize(request.getPhoneNumber()));
		address.setProvince(normalize(request.getProvince()));
		address.setDistrict(normalize(request.getDistrict()));
		address.setWard(normalize(request.getWard()));
		address.setDetailAddress(normalize(request.getDetailAddress()));
	}

	private String normalize(String value) {
		return value.trim().replaceAll("\\s+", " ");
	}
}
