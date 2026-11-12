package com.lauriel.store.dto.excel;

import java.io.Serializable;
import java.util.List;

public record ProductImportResult(
    boolean success,
    int totalProducts,
    int totalVariants,
    int validRows,
    int invalidRows,
    int createdProducts,
    int createdVariants,
    List<ImportRowError> errors,
    byte[] errorExcelBytes
) implements Serializable {

    public static ProductImportResult success(int createdProducts, int createdVariants) {
        int total = createdProducts + createdVariants;
        return new ProductImportResult(
            true,
            createdProducts,
            createdVariants,
            total,
            0,
            createdProducts,
            createdVariants,
            List.of(),
            null
        );
    }

    public static ProductImportResult failure(
        int totalProducts,
        int totalVariants,
        int validRows,
        int invalidRows,
        List<ImportRowError> errors,
        byte[] errorExcelBytes
    ) {
        return new ProductImportResult(
            false,
            totalProducts,
            totalVariants,
            validRows,
            invalidRows,
            0,
            0,
            errors != null ? errors : List.of(),
            errorExcelBytes
        );
    }

    public int errorCount() {
        return errors != null ? errors.size() : 0;
    }
}

