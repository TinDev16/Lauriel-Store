package com.lauriel.store.controller.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lauriel.store.entity.enums.ActiveStatus;
import com.lauriel.store.service.CategoryService;
import com.lauriel.store.service.ColorService;
import com.lauriel.store.service.ProductExcelExportService;
import com.lauriel.store.service.ProductExcelImportService;
import com.lauriel.store.service.ProductImageService;
import com.lauriel.store.service.ProductImageStorage;
import com.lauriel.store.service.ProductService;
import com.lauriel.store.service.ProductVariantService;
import java.io.OutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class WebProductExportControllerTest {

    private MockMvc mockMvc;
    private ProductExcelExportService exportService;
    private ProductExcelImportService importService;

    @BeforeEach
    void setUp() {
        ProductService productService = mock(ProductService.class);
        ProductVariantService variantService = mock(ProductVariantService.class);
        ProductImageService imageService = mock(ProductImageService.class);
        ProductImageStorage storage = mock(ProductImageStorage.class);
        CategoryService categoryService = mock(CategoryService.class);
        ColorService colorService = mock(ColorService.class);
        exportService = mock(ProductExcelExportService.class);
        importService = mock(ProductExcelImportService.class);

        WebProductController controller = new WebProductController(
            productService,
            variantService,
            imageService,
            storage,
            categoryService,
            colorService,
            exportService,
            importService
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /admin/products/export.xlsx returns 200, correct headers, and delegates to service")
    void exportExcelEndpointSuccess() throws Exception {
        doAnswer(invocation -> {
            OutputStream os = invocation.getArgument(3);
            os.write("test-excel-content".getBytes());
            return null;
        }).when(exportService).exportProducts(eq("ao"), eq(2L), eq(ActiveStatus.ACTIVE), any(OutputStream.class));

        var result = mockMvc.perform(get("/admin/products/export.xlsx")
                .param("keyword", "ao")
                .param("categoryId", "2")
                .param("status", "ACTIVE"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .andReturn();

        String disposition = result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION);
        assertThat(disposition).isNotNull();
        assertThat(disposition).startsWith("attachment; filename=\"products_");
        assertThat(disposition).endsWith(".xlsx\"");
        assertThat(result.getResponse().getContentAsString()).isEqualTo("test-excel-content");

        verify(exportService, times(1)).exportProducts(eq("ao"), eq(2L), eq(ActiveStatus.ACTIVE), any(OutputStream.class));
    }

    @Test
    @DisplayName("GET /admin/products/import-template.xlsx returns 200, attachment filename, and delegates to service")
    void downloadTemplateEndpoint() throws Exception {
        doAnswer(invocation -> {
            OutputStream os = invocation.getArgument(0);
            os.write("template-excel-content".getBytes());
            return null;
        }).when(importService).generateTemplate(any(OutputStream.class));

        var result = mockMvc.perform(get("/admin/products/import-template.xlsx"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"products_import_template.xlsx\""))
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEqualTo("template-excel-content");
        verify(importService, times(1)).generateTemplate(any(OutputStream.class));
    }

    @Test
    @DisplayName("POST /admin/products/import redirects to /admin/products and sets flash attributes")
    void importExcelEndpointSuccess() throws Exception {
        when(importService.importProducts(any()))
            .thenReturn(com.lauriel.store.dto.excel.ProductImportResult.success(3, 6));

        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
            "file", "products.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "content".getBytes()
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/admin/products/import").file(file))
            .andExpect(status().is3xxRedirection())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/admin/products"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attribute("importSuccess", true));

        verify(importService, times(1)).importProducts(any());
    }

    @Test
    @DisplayName("GET /admin/products/import-errors.xlsx downloads error file if session contains error bytes")
    void downloadErrorReportEndpoint() throws Exception {
        byte[] errorContent = "error-excel-content".getBytes();

        mockMvc.perform(get("/admin/products/import-errors.xlsx")
                .sessionAttr("LAST_IMPORT_ERROR_BYTES", errorContent))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("products_import_errors_")));
    }
}

