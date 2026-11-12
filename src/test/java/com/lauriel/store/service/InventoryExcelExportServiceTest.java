package com.lauriel.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lauriel.store.entity.*;
import com.lauriel.store.entity.enums.ActiveStatus;
import com.lauriel.store.entity.enums.InventoryTransactionType;
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

class InventoryExcelExportServiceTest {

    private InventoryService inventoryService;
    private InventoryExcelExportService exportService;

    @BeforeEach
    void setUp() {
        inventoryService = mock(InventoryService.class);
        exportService = new InventoryExcelExportService(inventoryService);
    }

    @Test
    @DisplayName("Xuất Excel Tồn kho tạo đầy đủ 2 sheet: Inventory và TransactionHistory")
    void testExportInventorySuccess() throws IOException {
        Product product = new Product();
        product.setProductId(15L);
        product.setProductName("Áo Polo Lauriel");

        Size size = new Size();
        size.setSizeId(1L);
        size.setSizeName("M");

        Color color = new Color();
        color.setColorId(2L);
        color.setColorName("Đen");

        ProductVariant variant = new ProductVariant();
        variant.setVariantId(401L);
        variant.setProduct(product);
        variant.setSku("POLO-BLK-M");
        variant.setSize(size);
        variant.setColor(color);
        variant.setPrice(BigDecimal.valueOf(320000));
        variant.setQuantity(45);
        variant.setStatus(ActiveStatus.ACTIVE);

        User user = new User();
        user.setUserId(3L);
        user.setFullName("Trần Thủ Kho");

        InventoryTransaction tx = new InventoryTransaction();
        tx.setTransactionId(701L);
        tx.setProductVariant(variant);
        tx.setUser(user);
        tx.setTransactionType(InventoryTransactionType.IMPORT);
        tx.setQuantity(50);
        tx.setReferenceType("PURCHASE_RECEIPT");
        tx.setReferenceId(101L);
        tx.setNote("Nhập hàng từ nhà cung cấp");
        tx.setCreatedAt(LocalDateTime.of(2026, 11, 5, 8, 0, 0));

        when(inventoryService.listVariantsForExport(any(), any(), any())).thenReturn(List.of(variant));
        when(inventoryService.listTransactionsForExport(any(), any(), any(), any(), any())).thenReturn(List.of(tx));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.exportInventory("Polo", 50, null, InventoryTransactionType.IMPORT, null, null, out);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(2);

            Sheet invSheet = wb.getSheet("Inventory");
            assertThat(invSheet).isNotNull();
            assertThat(invSheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("DANH SÁCH TỒN KHO THEO BIẾN THỂ");
            assertThat(invSheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Mã biến thể");
            assertThat(invSheet.getRow(4).getCell(0).getNumericCellValue()).isEqualTo(401.0);
            assertThat(invSheet.getRow(4).getCell(2).getStringCellValue()).isEqualTo("Áo Polo Lauriel");
            assertThat(invSheet.getRow(4).getCell(3).getStringCellValue()).isEqualTo("POLO-BLK-M");
            assertThat(invSheet.getRow(4).getCell(7).getNumericCellValue()).isEqualTo(45.0);
            assertThat(invSheet.getRow(4).getCell(8).getStringCellValue()).isEqualTo("Đang bán");

            Sheet txSheet = wb.getSheet("TransactionHistory");
            assertThat(txSheet).isNotNull();
            assertThat(txSheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("LỊCH SỬ GIAO DỊCH XUẤT NHẬP KHO");
            assertThat(txSheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Mã giao dịch");
            assertThat(txSheet.getRow(4).getCell(0).getNumericCellValue()).isEqualTo(701.0);
            assertThat(txSheet.getRow(4).getCell(2).getStringCellValue()).isEqualTo("Áo Polo Lauriel");
            assertThat(txSheet.getRow(4).getCell(4).getStringCellValue()).isEqualTo("Nhập hàng");
            assertThat(txSheet.getRow(4).getCell(5).getNumericCellValue()).isEqualTo(50.0);
            assertThat(txSheet.getRow(4).getCell(6).getStringCellValue()).isEqualTo("Trần Thủ Kho");
            assertThat(txSheet.getRow(4).getCell(7).getStringCellValue()).isEqualTo("PURCHASE_RECEIPT");
            assertThat(txSheet.getRow(4).getCell(8).getNumericCellValue()).isEqualTo(101.0);
        }
    }

    @Test
    @DisplayName("Xuất Excel Tồn kho khi danh sách rỗng vẫn tạo file hợp lệ")
    void testExportInventoryEmpty() throws IOException {
        when(inventoryService.listVariantsForExport(any(), any(), any())).thenReturn(List.of());
        when(inventoryService.listTransactionsForExport(any(), any(), any(), any(), any())).thenReturn(List.of());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.exportInventory(null, null, null, null, null, null, out);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(2);
            Sheet invSheet = wb.getSheet("Inventory");
            assertThat(invSheet.getRow(1).getCell(0).getStringCellValue()).contains("Tổng số: 0 biến thể");
        }
    }
}
