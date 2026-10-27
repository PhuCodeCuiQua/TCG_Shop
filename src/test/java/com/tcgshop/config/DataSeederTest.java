package com.tcgshop.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcgshop.entity.Role;
import com.tcgshop.entity.User;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.repository.CardRepository;
import com.tcgshop.repository.CommissionSettingRepository;
import com.tcgshop.repository.GameRepository;
import com.tcgshop.repository.PriceHistoryRepository;
import com.tcgshop.repository.ProductRepository;
import com.tcgshop.repository.RoleRepository;
import com.tcgshop.repository.SeriesRepository;
import com.tcgshop.repository.ShippingCarrierRepository;
import com.tcgshop.repository.ShopRepository;
import com.tcgshop.repository.UserRepository;
import java.util.HashSet;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {
	@Mock private RoleRepository roleRepository;
	@Mock private UserRepository userRepository;
	@Mock private ShopRepository shopRepository;
	@Mock private GameRepository gameRepository;
	@Mock private SeriesRepository seriesRepository;
	@Mock private CardRepository cardRepository;
	@Mock private ProductRepository productRepository;
	@Mock private ShippingCarrierRepository shippingCarrierRepository;
	@Mock private PriceHistoryRepository priceHistoryRepository;
	@Mock private CommissionSettingRepository commissionSettingRepository;
	@Mock private BCryptPasswordEncoder passwordEncoder;

	private DataSeeder dataSeeder;

	@BeforeEach
	void setUp() {
		dataSeeder = new DataSeeder(roleRepository, userRepository, shopRepository, gameRepository, seriesRepository,
				cardRepository, productRepository, shippingCarrierRepository, priceHistoryRepository,
				commissionSettingRepository, passwordEncoder);
	}

	@Test
	void suppliesSeedAddsOnlyMissingNames() {
		var shop = new com.tcgshop.entity.Shop();
		var existing = new com.tcgshop.entity.Product();
		existing.setName("Sleeves 100 nấm");
		when(productRepository.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<com.tcgshop.entity.Product>>any()))
				.thenReturn(java.util.List.of(existing));
		dataSeeder.seedSuppliesAndBundle(shop, null);
		var captured = org.mockito.ArgumentCaptor.forClass(com.tcgshop.entity.Product.class);
		verify(productRepository, org.mockito.Mockito.times(3)).save(captured.capture());
		assertThat(captured.getAllValues()).extracting(com.tcgshop.entity.Product::getName)
				.containsExactly("Deck Box gỗ", "Playmat TCG", "Bundle Pokémon Card 151");
		assertThat(captured.getAllValues()).allSatisfy(product -> {
			assertThat(product.getStock()).isPositive();
			assertThat(product.getShop()).isSameAs(shop);
		});
		when(productRepository.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<com.tcgshop.entity.Product>>any()))
				.thenReturn(java.util.List.of(existing, captured.getAllValues().get(0), captured.getAllValues().get(1), captured.getAllValues().get(2)));
		dataSeeder.seedSuppliesAndBundle(shop, null);
		verify(productRepository, org.mockito.Mockito.times(3)).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void reseedingAddsUserRoleToAnExistingDemoCustomer() {
		Role userRole = role(RoleName.ROLE_USER);
		User user = new User();
		user.setId(41L);
		user.setEmail("user1@tcgshop.local");
		when(userRepository.findByEmail("user1@tcgshop.local")).thenReturn(Optional.of(user));
		when(userRepository.save(user)).thenReturn(user);

		User seeded = dataSeeder.seedUser("user1", "Khách hàng Một", "0900000005", userRole);

		assertThat(seeded.getRoles()).contains(userRole);
		verify(userRepository).save(user);
	}

	@Test
	void reseedingAddsUserRoleToAnExistingVendorDemoAccount() {
		Role userRole = role(RoleName.ROLE_USER);
		Role vendorRole = role(RoleName.ROLE_VENDOR);
		User user = new User();
		user.setId(42L);
		user.setEmail("vendor1@tcgshop.local");
		user.setRoles(new HashSet<>());
		user.getRoles().add(vendorRole);
		when(userRepository.findByEmail("vendor1@tcgshop.local")).thenReturn(Optional.of(user));
		when(userRepository.save(user)).thenReturn(user);

		User seeded = dataSeeder.seedUser("vendor1", "Chủ shop Một", "0900000003", userRole, vendorRole);

		assertThat(seeded.getRoles()).contains(userRole, vendorRole);
		verify(userRepository).save(user);
	}

	@Test
	void reseedingDoesNotWriteWhenTheExpectedRoleAlreadyExists() {
		Role userRole = role(RoleName.ROLE_USER);
		User user = new User();
		user.setId(42L);
		user.setEmail("user1@tcgshop.local");
		user.getRoles().add(userRole);
		when(userRepository.findByEmail("user1@tcgshop.local")).thenReturn(Optional.of(user));

		assertThat(dataSeeder.seedUser("user1", "Khách hàng Một", "0900000005", userRole)).isSameAs(user);
	}

	private Role role(RoleName name) {
		Role role = new Role();
		role.setName(name);
		return role;
	}
}
