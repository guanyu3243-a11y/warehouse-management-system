package com.warehouse.management.service;

import com.warehouse.management.dto.CompanyProductImportResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CompanyProductImportService {

    CompanyProductImportResponse importProducts(Long categoryId, MultipartFile file);
}
