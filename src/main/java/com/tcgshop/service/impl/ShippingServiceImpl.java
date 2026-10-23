package com.tcgshop.service.impl;

import com.tcgshop.dto.request.ShippingCarrierRequest;
import com.tcgshop.dto.response.AdminShippingCarrierResponse;
import com.tcgshop.dto.response.ShippingCarrierResponse;
import com.tcgshop.entity.ShippingCarrier;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.BusinessException;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.exception.ResourceNotFoundException;
import com.tcgshop.mapper.ShippingCarrierMapper;
import com.tcgshop.repository.ShippingCarrierRepository;
import com.tcgshop.service.ShippingService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShippingServiceImpl implements ShippingService {
	// Bản tối thiểu do Phát thực hiện theo PA-18; đã áp dụng đúng quy tắc phí vận chuyển trong hợp đồng.
	private final ShippingCarrierRepository carrierRepository;
	private final ShippingCarrierMapper carrierMapper;

	public ShippingServiceImpl(ShippingCarrierRepository carrierRepository, ShippingCarrierMapper carrierMapper) {
		this.carrierRepository = carrierRepository;
		this.carrierMapper = carrierMapper;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ShippingCarrierResponse> getActiveCarriers() {
		return carrierRepository.findAllByActiveTrueOrderByNameAsc().stream().map(carrierMapper::toResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal calculateFee(Long carrierId, BigDecimal orderSubtotal) {
		ShippingCarrier carrier = carrierRepository.findById(carrierId)
				.orElseThrow(() -> new BadRequestException("Nhà vận chuyển không tồn tại hoặc đã ngừng hoạt động"));
		if (!carrier.isActive()) {
			throw new BadRequestException("Nhà vận chuyển không tồn tại hoặc đã ngừng hoạt động");
		}

		BigDecimal threshold = carrier.getFreeShipThreshold();
		if (threshold != null && orderSubtotal != null && orderSubtotal.compareTo(threshold) >= 0) {
			return BigDecimal.ZERO;
		}
		return carrier.getBaseFee();
	}

	@Override
	@Transactional(readOnly = true)
	public List<AdminShippingCarrierResponse> getAllForAdmin() {
		return carrierRepository.findAll().stream()
				.sorted((first, second) -> first.getName().compareToIgnoreCase(second.getName()))
				.map(this::toAdminResponse)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public AdminShippingCarrierResponse getForAdmin(Long carrierId) {
		return toAdminResponse(findOrThrow(carrierId));
	}

	@Override
	@Transactional
	public AdminShippingCarrierResponse create(ShippingCarrierRequest request) {
		validateRequest(request);
		rejectLogoUpload(request.getLogo());

		String name = request.getName().trim();
		String code = normalizeCode(request.getCode());
		ensureUnique(name, code, null);

		ShippingCarrier carrier = new ShippingCarrier();
		carrier.setName(name);
		carrier.setCode(code);
		carrier.setBaseFee(request.getBaseFee());
		carrier.setFreeShipThreshold(request.getFreeShipThreshold());
		carrier.setActive(request.isActive());
		return toAdminResponse(carrierRepository.save(carrier));
	}

	@Override
	@Transactional
	public AdminShippingCarrierResponse update(Long carrierId, ShippingCarrierRequest request) {
		validateRequest(request);
		rejectLogoUpload(request.getLogo());

		ShippingCarrier carrier = findOrThrow(carrierId);
		String name = request.getName().trim();
		String code = normalizeCode(request.getCode());
		ensureUnique(name, code, carrierId);

		carrier.setName(name);
		carrier.setCode(code);
		carrier.setBaseFee(request.getBaseFee());
		carrier.setFreeShipThreshold(request.getFreeShipThreshold());
		carrier.setActive(request.isActive());
		return toAdminResponse(carrier);
	}

	@Override
	@Transactional
	public boolean removeOrHide(Long carrierId) {
		ShippingCarrier carrier = findOrThrow(carrierId);
		boolean hasOrders = carrierRepository.countOrdersUsingCarrier(carrierId) > 0;

		// Không thể xóa an toàn logo Cloudinary khi entity chưa lưu public_id.
		if (hasOrders || (carrier.getLogoUrl() != null && !carrier.getLogoUrl().isBlank())) {
			carrier.setActive(false);
			return false;
		}
		carrierRepository.delete(carrier);
		return true;
	}

	private AdminShippingCarrierResponse toAdminResponse(ShippingCarrier carrier) {
		return carrierMapper.toAdminResponse(carrier, carrierRepository.countOrdersUsingCarrier(carrier.getId()));
	}

	private ShippingCarrier findOrThrow(Long carrierId) {
		return carrierRepository.findById(carrierId)
				.orElseThrow(() -> new ResourceNotFoundException("Nhà vận chuyển", carrierId));
	}

	private void validateRequest(ShippingCarrierRequest request) {
		if (request == null || request.getName() == null || request.getName().isBlank()
				|| request.getCode() == null || request.getCode().isBlank()) {
			throw new BadRequestException("Vui lòng nhập tên và mã nhà vận chuyển");
		}
		if (request.getBaseFee() == null || request.getBaseFee().signum() < 0
				|| (request.getFreeShipThreshold() != null && request.getFreeShipThreshold().signum() < 0)) {
			throw new BadRequestException("Phí vận chuyển và ngưỡng miễn phí không được âm");
		}
	}

	private void rejectLogoUpload(org.springframework.web.multipart.MultipartFile logo) {
		if (logo != null && !logo.isEmpty()) {
			// TODO(PA-18): Chỉ bật upload sau khi ShippingCarrier lưu logoPublicId để thay/xóa ảnh an toàn.
			throw new BusinessException("Tạm thời chưa thể tải logo lên; cần lưu mã ảnh Cloudinary trong cơ sở dữ liệu");
		}
	}

	private void ensureUnique(String name, String code, Long carrierId) {
		boolean duplicateName = carrierId == null
				? carrierRepository.existsByNameIgnoreCase(name)
				: carrierRepository.existsByNameIgnoreCaseAndIdNot(name, carrierId);
		boolean duplicateCode = carrierId == null
				? carrierRepository.existsByCodeIgnoreCase(code)
				: carrierRepository.existsByCodeIgnoreCaseAndIdNot(code, carrierId);

		if (duplicateName) {
			throw new ConflictException("Tên nhà vận chuyển đã tồn tại");
		}
		if (duplicateCode) {
			throw new ConflictException("Mã nhà vận chuyển đã tồn tại");
		}
	}

	private String normalizeCode(String code) {
		return code.trim().toUpperCase(Locale.ROOT);
	}
}
