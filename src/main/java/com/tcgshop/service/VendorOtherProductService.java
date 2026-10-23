package com.tcgshop.service;

import com.tcgshop.dto.request.VendorOtherProductRequest;
import com.tcgshop.dto.response.SeriesResponse;
import java.util.List;

public interface VendorOtherProductService {

	List<SeriesResponse> getActiveSeriesOptions();

	void createProduct(VendorOtherProductRequest request);
}
