package com.tcgshop.service.impl;

import com.tcgshop.dto.request.ShopRegistrationRequest;
import com.tcgshop.dto.response.ShopRegistrationResponse;
import com.tcgshop.entity.Shop;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.ShopStatus;
import com.tcgshop.exception.BadRequestException;
import com.tcgshop.exception.ConflictException;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.repository.UserRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.CloudinaryService;
import com.tcgshop.service.ShopRegistrationService;
import com.tcgshop.service.UploadResult;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShopRegistrationServiceImpl implements ShopRegistrationService {
	private static final String LOGO_FOLDER = "tcg-shop/shops";
	private static final Pattern NON_SLUG_CHARACTERS = Pattern.compile("[^a-z0-9]+");

	private final ShopRepository shopRepository;
	private final UserRepository userRepository;
	private final CloudinaryService cloudinaryService;
	private final CurrentUser currentUser;

	@Override
	@Transactional(readOnly = true)
	public Optional<ShopRegistrationResponse> getCurrentUserShop() {
		return shopRepository.findByOwnerId(currentUser.getId()).map(this::toResponse);
	}

	@Override
	@Transactional
	public ShopRegistrationResponse registerOrResubmit(ShopRegistrationRequest request) {
		Long ownerId = currentUser.getId();
		Shop shop = shopRepository.findByOwnerId(ownerId).orElse(null);
		if (shop != null && shop.getStatus() != ShopStatus.REJECTED) {
			throw new BadRequestException("Hồ sơ chi nhánh của bạn đã được gửi và không thể nộp lại ở trạng thái hiện tại");
		}

		String name = request.getName().trim();
		if (shopRepository.existsByName(name) && (shop == null || !shop.getName().equalsIgnoreCase(name))) {
			throw new ConflictException("Tên chi nhánh này đã được sử dụng. Vui lòng chọn tên khác.");
		}

		User owner = userRepository.findById(ownerId)
				.orElseThrow(() -> new BadRequestException("Không tìm thấy tài khoản đang đăng nhập"));
		shop = shop == null ? new Shop() : shop;
		shop.setOwner(owner);
		shop.setName(name);
		shop.setDescription(clean(request.getDescription()));
		shop.setPhoneNumber(request.getPhoneNumber().trim());
		shop.setPickupAddress(request.getPickupAddress().trim());
		shop.setStatus(ShopStatus.PENDING);
		shop.setRejectReason(null);
		shop.setSlug(createSlug(name, ownerId));

		if (request.getLogo() != null && !request.getLogo().isEmpty()) {
			UploadResult uploaded = cloudinaryService.uploadImage(request.getLogo(), LOGO_FOLDER);
			String previousPublicId = shop.getLogoPublicId();
			shop.setLogoUrl(uploaded.url());
			shop.setLogoPublicId(uploaded.publicId());
			Shop savedShop = shopRepository.save(shop);
			if (previousPublicId != null && !previousPublicId.isBlank()) {
				cloudinaryService.delete(previousPublicId, "image");
			}
			return toResponse(savedShop);
		}
		return toResponse(shopRepository.save(shop));
	}

	private String createSlug(String name, Long ownerId) {
		String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
				.replaceAll("\\p{M}+", "")
				.toLowerCase(Locale.ROOT);
		String base = NON_SLUG_CHARACTERS.matcher(normalized).replaceAll("-").replaceAll("^-|-$", "");
		if (base.isBlank()) {
			base = "shop";
		}
		return base + "-" + ownerId;
	}

	private String clean(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private ShopRegistrationResponse toResponse(Shop shop) {
		return new ShopRegistrationResponse(shop.getId(), shop.getName(), shop.getDescription(), shop.getLogoUrl(),
				shop.getPhoneNumber(), shop.getPickupAddress(), shop.getStatus(), shop.getRejectReason());
	}
}
