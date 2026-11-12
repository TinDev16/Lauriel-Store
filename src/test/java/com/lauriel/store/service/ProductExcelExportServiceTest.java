package com.lauriel.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lauriel.store.entity.Category;
import com.lauriel.store.entity.Color;
import com.lauriel.store.entity.Product;
import com.lauriel.store.entity.ProductVariant;
import com.lauriel.store.entity.Size;
import com.lauriel.store.entity.enums.ActiveStatus;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductExcelExportServiceTest {

    private ProductService productService;
    private ProductVariantService productVariantService;
    private ProductExcelExportService exportService;

    @BeforeEach
    void setUp() {
        productService = mock(ProductService.class);
        productVariantService = mock(ProductVariantService.class);
        exportService = new ProductExcelExportService(productService, productVariantService);
    }

    private Product createProduct(Long id, String name, String categoryName, BigDecimal price, ActiveStatus status) {
        Product p = new Product();
        p.setProductId(id);
        p.setProductName(name);
        p.setDescription("Mô tả cho " + name);
        p.setPrice(price);
        p.setStatus(status);
        p.setCreatedAt(LocalDateTime.of(2026, 11, 1, 10, 30, 0));

        Category cat = new Category();
        cat.setCategoryId(100L + id);
        cat.setCategoryName(categoryName);
        p.setCategory(cat);

        return p;
    }

    private ProductVariant createVariant(Long variantId, Product product, String sku, String sizeName, String colorName, BigDecimal price, int quantity) {
        ProductVariant v = new ProductVariant();
        v.setVariantId(variantId);
        v.setProduct(product);
        v.setSku(sku);

        Size s = new Size();
        s.setSizeId(10L);
        s.setSizeName(sizeName);
        v.setSize(s);

        Color c = new Color();
        c.setColorId(20L);
        c.setColorName(colorName);
        v.setColor(c);

        v.setPrice(price);
        v.setQuantity(quantity);
        v.setStatus(ActiveStatus.ACTIVE);
        return v;
    }

    @Test
    @DisplayName("1. Xuất khi không có bộ lọc: đầy đủ 2 sheet, đúng dữ liệu, đúng định dạng")
    void exportWithoutFilters() throws IOException {
        Product p1 = createProduct(1L, "Áo sơ mi lụa", "Áo", new BigDecimal("350000"), ActiveStatus.ACTIVE);
        Product p2 = createProduct(2L, "Quần tây âu", "Quần", new BigDecimal("450000"), ActiveStatus.ACTIVE);
        when(productService.listForExport(null, null, null)).thenReturn(List.of(p1, p2));

        ProductVariant v1 = createVariant(101L, p1, "SP-1-S1-C1", "M", "Trắng", new BigDecimal("350000"), 25);
        ProductVariant v2 = createVariant(102L, p1, "SP-1-S2-C1", "L", "Trắng", new BigDecimal("350000"), 10);
        ProductVariant v3 = createVariant(201L, p2, "SP-2-S1-C2", "32", "Đen", new BigDecimal("450000"), 15);
        when(productVariantService.listForExport(List.of(1L, 2L))).thenReturn(List.of(v1, v2, v3));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.exportProducts(null, null, null, out);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            // Sheet 1: Products
            Sheet productSheet = workbook.getSheet("Products");
            assertThat(productSheet).isNotNull();
            assertThat(productSheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("DANH SÁCH SẢN PHẨM");
            assertThat(productSheet.getRow(1).getCell(0).getStringCellValue()).contains("2 sản phẩm", "Tất cả");

            Row prodHeader = productSheet.getRow(3);
            assertThat(prodHeader.getCell(0).getStringCellValue()).isEqualTo("Mã sản phẩm");
            assertThat(prodHeader.getCell(1).getStringCellValue()).isEqualTo("Tên sản phẩm");
            assertThat(prodHeader.getCell(2).getStringCellValue()).isEqualTo("Danh mục");
            assertThat(prodHeader.getCell(3).getStringCellValue()).isEqualTo("Mô tả");
            assertThat(prodHeader.getCell(4).getStringCellValue()).isEqualTo("Giá sản phẩm");
            assertThat(prodHeader.getCell(5).getStringCellValue()).isEqualTo("Trạng thái");
            assertThat(prodHeader.getCell(6).getStringCellValue()).isEqualTo("Ngày tạo");

            // Row 4: p1
            Row r1 = productSheet.getRow(4);
            assertThat((long) r1.getCell(0).getNumericCellValue()).isEqualTo(1L);
            assertThat(r1.getCell(1).getStringCellValue()).isEqualTo("Áo sơ mi lụa");
            assertThat(r1.getCell(2).getStringCellValue()).isEqualTo("Áo");
            assertThat(r1.getCell(4).getNumericCellValue()).isEqualTo(350000.0);
            assertThat(r1.getCell(5).getStringCellValue()).isEqualTo("Đang hoạt động");

            // Freeze pane
            assertThat(productSheet.getPaneInformation().isFreezePane()).isTrue();

            // Sheet 2: Variants
            Sheet variantSheet = workbook.getSheet("Variants");
            assertThat(variantSheet).isNotNull();
            assertThat(variantSheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("DANH SÁCH BIẾN THỂ SẢN PHẨM");
            assertThat(variantSheet.getRow(1).getCell(0).getStringCellValue()).contains("3 biến thể", "Số lượng tồn kho chỉ phục vụ xem");

            Row varHeader = variantSheet.getRow(3);
            assertThat(varHeader.getCell(0).getStringCellValue()).isEqualTo("Mã biến thể");
            assertThat(varHeader.getCell(1).getStringCellValue()).isEqualTo("Mã sản phẩm");
            assertThat(varHeader.getCell(2).getStringCellValue()).isEqualTo("Tên sản phẩm");
            assertThat(varHeader.getCell(3).getStringCellValue()).isEqualTo("SKU");
            assertThat(varHeader.getCell(4).getStringCellValue()).isEqualTo("Kích thước");
            assertThat(varHeader.getCell(5).getStringCellValue()).isEqualTo("Màu sắc");
            assertThat(varHeader.getCell(6).getStringCellValue()).isEqualTo("Giá biến thể");
            assertThat(varHeader.getCell(7).getStringCellValue()).isEqualTo("Trạng thái");
            assertThat(varHeader.getCell(8).getStringCellValue()).isEqualTo("Số lượng tồn kho");

            // Variant row 4
            Row vr1 = variantSheet.getRow(4);
            assertThat((long) vr1.getCell(0).getNumericCellValue()).isEqualTo(101L);
            assertThat((long) vr1.getCell(1).getNumericCellValue()).isEqualTo(1L);
            assertThat(vr1.getCell(2).getStringCellValue()).isEqualTo("Áo sơ mi lụa");
            assertThat(vr1.getCell(3).getStringCellValue()).isEqualTo("SP-1-S1-C1");
            assertThat(vr1.getCell(4).getStringCellValue()).isEqualTo("M");
            assertThat(vr1.getCell(5).getStringCellValue()).isEqualTo("Trắng");
            assertThat(vr1.getCell(6).getNumericCellValue()).isEqualTo(350000.0);
            assertThat(vr1.getCell(7).getStringCellValue()).isEqualTo("Đang hoạt động");
            assertThat((int) vr1.getCell(8).getNumericCellValue()).isEqualTo(25);
        }
    }

    @Test
    @DisplayName("2. Xuất khi có bộ lọc: keyword, categoryId, status")
    void exportWithFilters() throws IOException {
        String keyword = "váy";
        Long categoryId = 5L;
        ActiveStatus status = ActiveStatus.ACTIVE;

        Product p1 = createProduct(10L, "Váy dạ hội", "Đầm", new BigDecimal("1200000"), ActiveStatus.ACTIVE);
        when(productService.listForExport(keyword, categoryId, status)).thenReturn(List.of(p1));
        when(productVariantService.listForExport(List.of(10L))).thenReturn(List.of());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.exportProducts(keyword, categoryId, status, out);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            Sheet sheet = workbook.getSheet("Products");
            String subtitle = sheet.getRow(1).getCell(0).getStringCellValue();
            assertThat(subtitle).contains("Từ khóa: \"váy\"", "Danh mục ID: #5", "Trạng thái: Đang hoạt động");
        }
    }

    @Test
    @DisplayName("3. Xuất khi dữ liệu vượt quá một trang: xuất toàn bộ dữ liệu (25 sản phẩm > pageSize 12)")
    void exportWhenDataExceedsOnePage() throws IOException {
        List<Product> largeList = new ArrayList<>();
        List<Long> productIds = new ArrayList<>();
        for (long i = 1; i <= 25; i++) {
            largeList.add(createProduct(i, "Sản phẩm " + i, "Danh mục test", new BigDecimal("100000"), ActiveStatus.ACTIVE));
            productIds.add(i);
        }
        when(productService.listForExport(null, null, null)).thenReturn(largeList);
        when(productVariantService.listForExport(productIds)).thenReturn(List.of());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.exportProducts(null, null, null, out);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            Sheet sheet = workbook.getSheet("Products");
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(25 + 4); // row 0 (title), row 1 (sub), row 2 (blank), row 3 (header), row 4..28 (25 data)
            Row lastRow = sheet.getRow(28);
            assertThat(lastRow).isNotNull();
            assertThat((long) lastRow.getCell(0).getNumericCellValue()).isEqualTo(25L);
            assertThat(lastRow.getCell(1).getStringCellValue()).isEqualTo("Sản phẩm 25");
        }
    }

    @Test
    @DisplayName("4. Xuất khi không có dữ liệu: file vẫn có tiêu đề và header, không lỗi")
    void exportWhenNoData() throws IOException {
        when(productService.listForExport("không-tồn-tại", null, null)).thenReturn(List.of());
        when(productVariantService.listForExport(List.of())).thenReturn(List.of());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.exportProducts("không-tồn-tại", null, null, out);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            Sheet productSheet = workbook.getSheet("Products");
            assertThat(productSheet).isNotNull();
            assertThat(productSheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Mã sản phẩm");
            // No data rows (only header at row 3)
            assertThat(productSheet.getRow(4)).isNull();

            Sheet variantSheet = workbook.getSheet("Variants");
            assertThat(variantSheet).isNotNull();
            assertThat(variantSheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Mã biến thể");
            assertThat(variantSheet.getRow(4)).isNull();
        }
    }

    @Test
    @DisplayName("5. Kiểm tra file mở được bằng Microsoft Excel (OOXML XSSFWorkbook, Zip Package hợp lệ)")
    void fileCanBeOpenedByMicrosoftExcel() throws IOException {
        Product p = createProduct(1L, "Áo khoác gió Nam", "Áo khoác", new BigDecimal("550000"), ActiveStatus.ACTIVE);
        when(productService.listForExport(null, null, null)).thenReturn(List.of(p));
        when(productVariantService.listForExport(List.of(1L))).thenReturn(List.of());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.exportProducts(null, null, null, out);

        byte[] bytes = out.toByteArray();
        assertThat(bytes.length).isGreaterThan(1000);

        // Verify it is a valid Zip file (OOXML .xlsx format starting with PK magic bytes 0x50, 0x4B, 0x03, 0x04)
        assertThat(bytes[0]).isEqualTo((byte) 'P');
        assertThat(bytes[1]).isEqualTo((byte) 'K');

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            assertThat(workbook.getSheetName(0)).isEqualTo("Products");
            assertThat(workbook.getSheetName(1)).isEqualTo("Variants");
        }
    }
}

