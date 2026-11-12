package com.lauriel.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lauriel.store.entity.*;
import com.lauriel.store.entity.enums.PurchaseReceiptStatus;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PurchaseReceiptExcelExportServiceTest {

    private PurchaseReceiptService receiptService;
    private PurchaseReceiptExcelExportService exportService;

    @BeforeEach
    void setUp() {
        receiptService = mock(PurchaseReceiptService.class);
        exportService = new PurchaseReceiptExcelExportService(receiptService);
    }

    @Test
    @DisplayName("Xuất Excel Phiếu nhập tạo đầy đủ 2 sheet: PurchaseReceipts và PurchaseReceiptDetails")
    void testExportReceiptsSuccess() throws IOException {
        PurchaseReceipt pr = new PurchaseReceipt();
        pr.setReceiptId(501L);
        pr.setStatus(PurchaseReceiptStatus.COMPLETED);
        pr.setTotalAmount(BigDecimal.valueOf(12000000));
        pr.setCreatedAt(LocalDateTime.of(2026, 11, 8, 9, 30, 0));

        Supplier supplier = new Supplier();
        supplier.setSupplierId(10L);
        supplier.setSupplierName("Công ty Cung ứng May mặc Việt Hàn");
        pr.setSupplier(supplier);

        User user = new User();
        user.setUserId(2L);
        user.setFullName("Lê Quản Kho");
        user.setUsername("kho01");
        pr.setUser(user);

        Product product = new Product();
        product.setProductId(50L);
        product.setProductName("Quần Jean Nam Lauriel");

        Size size = new Size();
        size.setSizeId(2L);
        size.setSizeName("XL");

        Color color = new Color();
        color.setColorId(3L);
        color.setColorName("Xanh Đậm");

        ProductVariant variant = new ProductVariant();
        variant.setVariantId(301L);
        variant.setSku("QJ-BLUE-XL");
        variant.setProduct(product);
        variant.setSize(size);
        variant.setColor(color);

        PurchaseReceiptDetail detail = new PurchaseReceiptDetail();
        detail.setId(new PurchaseReceiptDetailId(501L, 301L));
        detail.setPurchaseReceipt(pr);
        detail.setProductVariant(variant);
        detail.setQuantity(100);
        detail.setUnitPrice(BigDecimal.valueOf(120000));
        detail.setSubtotal(BigDecimal.valueOf(12000000));

        when(receiptService.listForExport(any(), any(), any(), any())).thenReturn(List.of(pr));
        when(receiptService.listDetailsForExport(any())).thenReturn(List.of(detail));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.exportReceipts("Việt Hàn", PurchaseReceiptStatus.COMPLETED, null, null, out);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(2);

            Sheet receiptsSheet = wb.getSheet("PurchaseReceipts");
            assertThat(receiptsSheet).isNotNull();
            assertThat(receiptsSheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("DANH SÁCH PHIẾU NHẬP HÀNG");
            assertThat(receiptsSheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Mã phiếu nhập");
            assertThat(receiptsSheet.getRow(4).getCell(0).getStringCellValue()).isEqualTo("PN-501");
            assertThat(receiptsSheet.getRow(4).getCell(1).getStringCellValue()).contains("Việt Hàn");
            assertThat(receiptsSheet.getRow(4).getCell(2).getStringCellValue()).isEqualTo("Lê Quản Kho");
            assertThat(receiptsSheet.getRow(4).getCell(4).getStringCellValue()).isEqualTo("Đã hoàn tất");
            assertThat(receiptsSheet.getRow(4).getCell(5).getNumericCellValue()).isEqualTo(12000000.0);

            Sheet detailsSheet = wb.getSheet("PurchaseReceiptDetails");
            assertThat(detailsSheet).isNotNull();
            assertThat(detailsSheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("CHI TIẾT MẶT HÀNG NHẬP KHO");
            assertThat(detailsSheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Mã phiếu nhập");
            assertThat(detailsSheet.getRow(4).getCell(0).getStringCellValue()).isEqualTo("PN-501");
            assertThat(detailsSheet.getRow(4).getCell(1).getStringCellValue()).isEqualTo("Quần Jean Nam Lauriel");
            assertThat(detailsSheet.getRow(4).getCell(2).getStringCellValue()).isEqualTo("QJ-BLUE-XL");
            assertThat(detailsSheet.getRow(4).getCell(5).getNumericCellValue()).isEqualTo(100.0);
        }
    }

    @Test
    @DisplayName("Xuất Excel Phiếu nhập khi danh sách rỗng vẫn tạo file hợp lệ")
    void testExportReceiptsEmpty() throws IOException {
        when(receiptService.listForExport(any(), any(), any(), any())).thenReturn(List.of());
        when(receiptService.listDetailsForExport(any())).thenReturn(List.of());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.exportReceipts(null, null, null, null, out);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(2);
            Sheet receiptsSheet = wb.getSheet("PurchaseReceipts");
            assertThat(receiptsSheet.getRow(1).getCell(0).getStringCellValue()).contains("Tổng số: 0 phiếu nhập");
        }
    }
}
