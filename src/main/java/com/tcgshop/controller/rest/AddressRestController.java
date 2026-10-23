package com.tcgshop.controller.rest;

import com.tcgshop.dto.response.AddressResponse;
import com.tcgshop.service.AddressService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile/addresses")
public class AddressRestController {
	private final AddressService addressService;

	public AddressRestController(AddressService addressService) {
		this.addressService = addressService;
	}

	@GetMapping
	public List<AddressResponse> list() {
		return addressService.list();
	}
}
