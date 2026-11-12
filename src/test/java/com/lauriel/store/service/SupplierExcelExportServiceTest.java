package com.lauriel.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lauriel.store.entity.Supplier;
import com.lauriel.store.entity.enums.ActiveStatus;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SupplierExcelExportServiceTest {

    private SupplierService supplierService;
    private SupplierExcelExportService exportService;

    @BeforeEach
    void setUp() {
        supplierService = mock(SupplierService.class);
        exportService = new SupplierExcelExportService(supplierService);
    }

    @Test
    @DisplayName("Xuất Excel Nhà cung cấp tạo sheet Suppliers với thông tin chi tiết đầy đủ")
    void testExportSuppliersSuccess() throws IOException {
        Supplier supplier = new Supplier();
        supplier.setSupplierId(12L);
        supplier.setSupplierName("Công ty Dệt May Miền Nam");
        supplier.setPhone("02838999999");
        supplier.setEmail("lienhe@detmaymiennam.com");
        supplier.setAddress("123 Võ Văn Kiệt, Q.1, TP.HCM");
        supplier.setStatus(ActiveStatus.ACTIVE);

        when(supplierService.listForExport(any(), any())).thenReturn(List.of(supplier));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.exportSuppliers("Miền Nam", ActiveStatus.ACTIVE, out);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(1);

            Sheet sheet = wb.getSheet("Suppliers");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("DANH SÁCH NHÀ CUNG CẤP");
            assertThat(sheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Mã nhà cung cấp");
            assertThat(sheet.getRow(4).getCell(0).getNumericCellValue()).isEqualTo(12.0);
            assertThat(sheet.getRow(4).getCell(1).getStringCellValue()).isEqualTo("Công ty Dệt May Miền Nam");
            assertThat(sheet.getRow(4).getCell(2).getStringCellValue()).contains("02838999999 / lienhe@detmaymiennam.com");
            assertThat(sheet.getRow(4).getCell(3).getStringCellValue()).isEqualTo("02838999999");
            assertThat(sheet.getRow(4).getCell(4).getStringCellValue()).isEqualTo("lienhe@detmaymiennam.com");
            assertThat(sheet.getRow(4).getCell(5).getStringCellValue()).isEqualTo("123 Võ Văn Kiệt, Q.1, TP.HCM");
            assertThat(sheet.getRow(4).getCell(6).getStringCellValue()).isEqualTo("Đang hợp tác");
        }
    }

    @Test
    @DisplayName("Xuất Excel Nhà cung cấp khi danh sách rỗng vẫn tạo file hợp lệ")
    void testExportSuppliersEmpty() throws IOException {
        when(supplierService.listForExport(any(), any())).thenReturn(List.of());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.exportSuppliers(null, null, out);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(1);
            Sheet sheet = wb.getSheet("Suppliers");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).contains("Tổng số: 0 nhà cung cấp");
        }
    }
}

