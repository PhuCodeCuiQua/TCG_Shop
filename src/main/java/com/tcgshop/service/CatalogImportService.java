package com.tcgshop.service;

import com.tcgshop.dto.request.CatalogImportRequest;
import com.tcgshop.dto.response.CatalogImportResponse;

public interface CatalogImportService {
    CatalogImportResponse importItem(CatalogImportRequest request);
}
