package com.lauriel.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lauriel.store.entity.Customer;
import com.lauriel.store.entity.User;
import com.lauriel.store.entity.enums.UserStatus;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CustomerExcelExportServiceTest {

    private CustomerService customerService;
    private CustomerExcelExportService exportService;

    @BeforeEach
    void setUp() {
        customerService = mock(CustomerService.class);
        exportService = new CustomerExcelExportService(customerService);
    }

    @Test
    @DisplayName("Xuất Excel Khách hàng tạo sheet Customers với thông tin hợp lệ và không chứa thông tin xác thực")
    void testExportCustomersSuccess() throws IOException {
        User user = new User();
        user.setUserId(15L);
        user.setUsername("khachhang01");
        user.setEmail("khachhang01@gmail.com");
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash("SENSITIVE_BCRYPT_HASH_SHOULD_NOT_LEAK");

        Customer customer = new Customer();
        customer.setCustomerId(88L);
        customer.setCustomerName("Phạm Thị Bích");
        customer.setPhone("0912345678");
        customer.setUser(user);
        customer.setCreatedAt(LocalDateTime.of(2026, 11, 2, 16, 20, 0));

        when(customerService.listForExport(any(), any())).thenReturn(List.of(customer));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.exportCustomers("Bích", UserStatus.ACTIVE, out);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(1);

            Sheet sheet = wb.getSheet("Customers");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("DANH SÁCH KHÁCH HÀNG");
            assertThat(sheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Mã khách hàng");
            assertThat(sheet.getRow(4).getCell(0).getNumericCellValue()).isEqualTo(88.0);
            assertThat(sheet.getRow(4).getCell(1).getStringCellValue()).isEqualTo("Phạm Thị Bích");
            assertThat(sheet.getRow(4).getCell(2).getStringCellValue()).isEqualTo("0912345678");
            assertThat(sheet.getRow(4).getCell(3).getStringCellValue()).isEqualTo("khachhang01@gmail.com");
            assertThat(sheet.getRow(4).getCell(4).getStringCellValue()).contains("02/11/2026");

            // Verify no sensitive credentials leaked anywhere in sheet
            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                var row = sheet.getRow(r);
                if (row == null) continue;
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    var cell = row.getCell(c);
                    if (cell != null && cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                        assertThat(cell.getStringCellValue()).doesNotContain("SENSITIVE_BCRYPT_HASH");
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("Xuất Excel Khách hàng khi danh sách rỗng vẫn tạo file hợp lệ")
    void testExportCustomersEmpty() throws IOException {
        when(customerService.listForExport(any(), any())).thenReturn(List.of());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.exportCustomers(null, null, out);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(1);
            Sheet sheet = wb.getSheet("Customers");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).contains("Tổng số: 0 khách hàng");
        }
    }
}

