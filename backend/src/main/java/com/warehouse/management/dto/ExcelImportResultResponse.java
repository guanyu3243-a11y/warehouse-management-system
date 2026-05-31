package com.warehouse.management.dto;

import java.util.List;

public record ExcelImportResultResponse(
        Integer totalCount,
        Integer successCount,
        Integer failCount,
        List<ExcelImportFailure> failures
) {

    public record ExcelImportFailure(
            Integer rowNumber,
            String reason
    ) {
    }
}
