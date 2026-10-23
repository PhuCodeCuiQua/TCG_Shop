package com.tcgshop.service;

import com.tcgshop.dto.request.AddressRequest;
import com.tcgshop.dto.response.AddressResponse;
import java.util.List;
import java.util.Optional;

public interface AddressService {
	List<AddressResponse> list();
	AddressResponse get(Long id);
	AddressResponse getForCheckout(Long id);
	Optional<AddressResponse> findDefault();
	AddressResponse create(AddressRequest request);
	AddressResponse update(Long id, AddressRequest request);
	AddressResponse setDefault(Long id);
	void delete(Long id);
}
