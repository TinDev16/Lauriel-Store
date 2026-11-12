package com.lauriel.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.lauriel.store.dto.excel.ProductImportResult;
import com.lauriel.store.dto.request.ProductRequest;
import com.lauriel.store.dto.request.ProductVariantRequest;
import com.lauriel.store.dto.response.CategorySummaryResponse;
import com.lauriel.store.dto.response.ProductResponse;
import com.lauriel.store.entity.Category;
import com.lauriel.store.entity.Color;
import com.lauriel.store.entity.Size;
import com.lauriel.store.entity.enums.ActiveStatus;
import com.lauriel.store.repository.CategoryRepository;
import com.lauriel.store.repository.ColorRepository;
import com.lauriel.store.repository.ProductVariantRepository;
import com.lauriel.store.repository.SizeRepository;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ProductExcelImportServiceTest {

    private ProductService productService;
    private ProductVariantService productVariantService;
    private ProductVariantRepository productVariantRepository;
    private CategoryRepository categoryRepository;
    private SizeRepository sizeRepository;
    private ColorRepository colorRepository;

    private ProductExcelImportService importService;

    @BeforeEach
    void setUp() {
        productService = mock(ProductService.class);
        productVariantService = mock(ProductVariantService.class);
        productVariantRepository = mock(ProductVariantRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        sizeRepository = mock(SizeRepository.class);
        colorRepository = mock(ColorRepository.class);

        importService = new ProductExcelImportService(
            productService,
            productVariantService,
            productVariantRepository,
            categoryRepository,
            sizeRepository,
            colorRepository
        );

        // Standard mock reference data
        Category catActive = new Category();
        catActive.setCategoryId(1L);
        catActive.setCategoryName("Áo");
        catActive.setStatus(ActiveStatus.ACTIVE);

        Category catInactive = new Category();
        catInactive.setCategoryId(2L);
        catInactive.setCategoryName("Quần Cũ");
        catInactive.setStatus(ActiveStatus.INACTIVE);

        when(categoryRepository.findAll()).thenReturn(List.of(catActive, catInactive));

        Size size1 = new Size();
        size1.setSizeId(10L);
        size1.setSizeName("S");
        Size size2 = new Size();
        size2.setSizeId(11L);
        size2.setSizeName("M");
        when(sizeRepository.findAll()).thenReturn(List.of(size1, size2));

        Color col1 = new Color();
        col1.setColorId(20L);
        col1.setColorName("Trắng");
        Color col2 = new Color();
        col2.setColorId(21L);
        col2.setColorName("Đen");
        when(colorRepository.findAll()).thenReturn(List.of(col1, col2));
    }

    private byte[] createExcel(List<String[]> products, List<String[]> variants) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Sheet Products
            Sheet pSheet = workbook.createSheet("Products");
            Row pHead = pSheet.createRow(0);
            String[] pCols = { "product_key", "product_name", "category_id", "description", "base_price", "status" };
            for (int i = 0; i < pCols.length; i++) pHead.createCell(i).setCellValue(pCols[i]);
            if (products != null) {
                for (int r = 0; r < products.size(); r++) {
                    Row row = pSheet.createRow(r + 1);
                    String[] data = products.get(r);
                    for (int c = 0; c < data.length; c++) row.createCell(c).setCellValue(data[c]);
                }
            }

            // Sheet Variants
            Sheet vSheet = workbook.createSheet("Variants");
            Row vHead = vSheet.createRow(0);
            String[] vCols = { "product_key", "sku", "size_id", "color_id", "variant_price", "status" };
            for (int i = 0; i < vCols.length; i++) vHead.createCell(i).setCellValue(vCols[i]);
            if (variants != null) {
                for (int r = 0; r < variants.size(); r++) {
                    Row row = vSheet.createRow(r + 1);
                    String[] data = variants.get(r);
                    for (int c = 0; c < data.length; c++) row.createCell(c).setCellValue(data[c]);
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    @Test
    @DisplayName("1. Tạo template mẫu: đủ 3 sheet Products, Variants, Huong_Dan_Tham_Chieu")
    void generateTemplate() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        importService.generateTemplate(baos);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(baos.toByteArray()))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(3);
            assertThat(wb.getSheetName(0)).isEqualTo("Products");
            assertThat(wb.getSheetName(1)).isEqualTo("Variants");
            assertThat(wb.getSheetName(2)).isEqualTo("Huong_Dan_Tham_Chieu");

            Sheet pSheet = wb.getSheet("Products");
            Row pHead = pSheet.getRow(0);
            assertThat(pHead.getCell(0).getStringCellValue()).isEqualTo("product_key");
            assertThat(pHead.getCell(1).getStringCellValue()).isEqualTo("product_name");
            assertThat(pHead.getCell(2).getStringCellValue()).isEqualTo("category");
            assertThat(pHead.getCell(4).getStringCellValue()).isEqualTo("base_price");

            Sheet vSheet = wb.getSheet("Variants");
            Row vHead = vSheet.getRow(0);
            assertThat(vHead.getCell(0).getStringCellValue()).isEqualTo("product_key");
            assertThat(vHead.getCell(1).getStringCellValue()).isEqualTo("sku");
            assertThat(vHead.getCell(2).getStringCellValue()).isEqualTo("size");
            assertThat(vHead.getCell(3).getStringCellValue()).isEqualTo("color");
        }
    }

    @Test
    @DisplayName("2b. Nhập file hợp lệ với Tên trực tiếp: category='Áo', size='S', color='Trắng'")
    void importSuccessWithNames() throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet pSheet = wb.createSheet("Products");
            Row pHead = pSheet.createRow(0);
            String[] pCols = { "product_key", "product_name", "category", "description", "base_price", "status" };
            for (int i = 0; i < pCols.length; i++) pHead.createCell(i).setCellValue(pCols[i]);
            Row pRow = pSheet.createRow(1);
            pRow.createCell(0).setCellValue("P01");
            pRow.createCell(1).setCellValue("Áo Polo Nam");
            pRow.createCell(2).setCellValue("Áo");
            pRow.createCell(3).setCellValue("Mô tả");
            pRow.createCell(4).setCellValue(250000.0);
            pRow.createCell(5).setCellValue("ACTIVE");

            Sheet vSheet = wb.createSheet("Variants");
            Row vHead = vSheet.createRow(0);
            String[] vCols = { "product_key", "sku", "size", "color", "variant_price", "status" };
            for (int i = 0; i < vCols.length; i++) vHead.createCell(i).setCellValue(vCols[i]);
            Row vRow = vSheet.createRow(1);
            vRow.createCell(0).setCellValue("P01");
            vRow.createCell(1).setCellValue("P01-S-TRANG");
            vRow.createCell(2).setCellValue("S");
            vRow.createCell(3).setCellValue("Trắng");
            vRow.createCell(4).setCellValue(250000.0);
            vRow.createCell(5).setCellValue("ACTIVE");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);

            when(productVariantRepository.existsBySkuIgnoreCase(any())).thenReturn(false);
            CategorySummaryResponse catResp = new CategorySummaryResponse(1L, "Áo");
            when(productService.create(any(ProductRequest.class)))
                .thenReturn(new ProductResponse(100L, "Áo Polo Nam", catResp, "Mô tả", new BigDecimal("250000"), null, ActiveStatus.ACTIVE));

            MockMultipartFile file = new MockMultipartFile("file", "test_names.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", baos.toByteArray());
            ProductImportResult result = importService.importProducts(file);

            assertThat(result.success()).isTrue();
            assertThat(result.createdProducts()).isEqualTo(1);
            assertThat(result.createdVariants()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("2. Nhập file hợp lệ: tạo sản phẩm và biến thể thành công, đúng transaction")
    void importSuccess() throws IOException {
        List<String[]> products = List.of(
            new String[]{ "P01", "Áo sơ mi Oxford", "1", "Vải cotton thoáng mát", "350000", "ACTIVE" },
            new String[]{ "P02", "Áo thun basic", "1", "Form rộng", "180000", "ACTIVE" }
        );
        List<String[]> variants = List.of(
            new String[]{ "P01", "P01-S-TRANG", "10", "20", "350000", "ACTIVE" },
            new String[]{ "P01", "P01-M-TRANG", "11", "20", "350000", "ACTIVE" },
            new String[]{ "P02", "P02-S-DEN", "10", "21", "180000", "ACTIVE" }
        );

        when(productVariantRepository.existsBySkuIgnoreCase(any())).thenReturn(false);

        CategorySummaryResponse catResp = new CategorySummaryResponse(1L, "Áo");
        when(productService.create(any(ProductRequest.class)))
            .thenReturn(new ProductResponse(100L, "Áo sơ mi Oxford", catResp, "desc", new BigDecimal("350000"), null, ActiveStatus.ACTIVE))
            .thenReturn(new ProductResponse(101L, "Áo thun basic", catResp, "desc", new BigDecimal("180000"), null, ActiveStatus.ACTIVE));

        byte[] content = createExcel(products, variants);
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);

        ProductImportResult result = importService.importProducts(file);

        assertThat(result.success()).isTrue();
        assertThat(result.createdProducts()).isEqualTo(2);
        assertThat(result.createdVariants()).isEqualTo(3);
        assertThat(result.errors()).isEmpty();

        verify(productService, times(2)).create(any(ProductRequest.class));
        verify(productVariantService, times(3)).create(any(Long.class), any(ProductVariantRequest.class));
    }

    @Test
    @DisplayName("3. Lỗi: File rỗng hoặc không đúng định dạng .xlsx")
    void importEmptyOrWrongFormat() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]);
        ProductImportResult resEmpty = importService.importProducts(emptyFile);
        assertThat(resEmpty.success()).isFalse();
        assertThat(resEmpty.errors().get(0).message()).contains("Vui lòng chọn file Excel");

        MockMultipartFile csvFile = new MockMultipartFile("file", "test.csv", "text/csv", "col1,col2".getBytes());
        ProductImportResult resCsv = importService.importProducts(csvFile);
        assertThat(resCsv.success()).isFalse();
        assertThat(resCsv.errors().get(0).message()).contains("Chỉ chấp nhận file định dạng .xlsx");
    }

    @Test
    @DisplayName("4. Lỗi: Trùng product_key trong file, trùng SKU trong file, SKU đã tồn tại trong DB")
    void importDuplicateKeysAndSkus() throws IOException {
        List<String[]> products = List.of(
            new String[]{ "P01", "Sản phẩm 1", "1", "desc", "100000", "ACTIVE" },
            new String[]{ "P01", "Sản phẩm 1 trùng", "1", "desc", "100000", "ACTIVE" } // trùng product_key
        );
        List<String[]> variants = List.of(
            new String[]{ "P01", "SKU-TRUNG", "10", "20", "100000", "ACTIVE" },
            new String[]{ "P01", "SKU-TRUNG", "11", "20", "100000", "ACTIVE" }, // trùng SKU trong file
            new String[]{ "P01", "SKU-DA-CO", "10", "21", "100000", "ACTIVE" }
        );

        when(productVariantRepository.existsBySkuIgnoreCase("SKU-DA-CO")).thenReturn(true);

        byte[] content = createExcel(products, variants);
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);

        ProductImportResult result = importService.importProducts(file);

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).isNotEmpty();

        boolean hasDupProdKey = result.errors().stream().anyMatch(e -> e.message().contains("product_key 'P01' bị trùng lặp"));
        boolean hasDupSkuInFile = result.errors().stream().anyMatch(e -> e.message().contains("SKU 'SKU-TRUNG' bị trùng lặp"));
        boolean hasExistingSkuInDb = result.errors().stream().anyMatch(e -> e.message().contains("SKU 'SKU-DA-CO' đã tồn tại"));

        assertThat(hasDupProdKey).isTrue();
        assertThat(hasDupSkuInFile).isTrue();
        assertThat(hasExistingSkuInDb).isTrue();

        // Ensure zero DB writes occurred
        verify(productService, never()).create(any());
        verify(productVariantService, never()).create(any(), any());
    }

    @Test
    @DisplayName("5. Lỗi: Danh mục không tồn tại / ngừng hoạt động, Size/Color không tồn tại, product_key biến thể không khớp")
    void importInvalidReferences() throws IOException {
        List<String[]> products = List.of(
            new String[]{ "P01", "Sản phẩm A", "999", "desc", "100000", "ACTIVE" }, // category 999 không có
            new String[]{ "P02", "Sản phẩm B", "2", "desc", "100000", "ACTIVE" }   // category 2 ngừng hoạt động
        );
        List<String[]> variants = List.of(
            new String[]{ "P99", "SKU-01", "10", "20", "100000", "ACTIVE" }, // P99 không tồn tại trong Products
            new String[]{ "P01", "SKU-02", "888", "20", "100000", "ACTIVE" }, // size 888 không có
            new String[]{ "P01", "SKU-03", "10", "777", "100000", "ACTIVE" }  // color 777 không có
        );

        byte[] content = createExcel(products, variants);
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);

        ProductImportResult result = importService.importProducts(file);

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).isNotEmpty();

        boolean hasCatNotExist = result.errors().stream().anyMatch(e -> e.message().contains("Danh mục #999 không tồn tại"));
        boolean hasCatInactive = result.errors().stream().anyMatch(e -> e.message().contains("đang ngừng hoạt động"));
        boolean hasUnknownProdKey = result.errors().stream().anyMatch(e -> e.message().contains("product_key 'P99' không tồn tại"));
        boolean hasSizeNotExist = result.errors().stream().anyMatch(e -> e.message().contains("Kích cỡ #888 không tồn tại"));
        boolean hasColorNotExist = result.errors().stream().anyMatch(e -> e.message().contains("Màu sắc #777 không tồn tại"));

        assertThat(hasCatNotExist).isTrue();
        assertThat(hasCatInactive).isTrue();
        assertThat(hasUnknownProdKey).isTrue();
        assertThat(hasSizeNotExist).isTrue();
        assertThat(hasColorNotExist).isTrue();

        // Verify error report Excel bytes generated
        assertThat(result.errorExcelBytes()).isNotNull();
        assertThat(result.errorExcelBytes().length).isGreaterThan(1000);
    }
}

