package com.lauriel.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lauriel.store.entity.*;
import com.lauriel.store.entity.enums.OrderStatus;
import com.lauriel.store.entity.enums.SalesChannel;
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

class OrderExcelExportServiceTest {

    private OrderService orderService;
    private OrderExcelExportService exportService;

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        exportService = new OrderExcelExportService(orderService);
    }

    @Test
    @DisplayName("Xuất Excel Đơn hàng tạo đầy đủ 2 sheet: Orders và OrderDetails")
    void testExportOrdersSuccess() throws IOException {
        Order order = new Order();
        order.setOrderId(101L);
        order.setStatus(OrderStatus.DELIVERED);
        order.setTotalAmount(BigDecimal.valueOf(500000));
        order.setDiscountAmount(BigDecimal.valueOf(50000));
        order.setShippingFee(BigDecimal.valueOf(30000));
        order.setFinalAmount(BigDecimal.valueOf(480000));
        order.setSalesChannel(SalesChannel.WEB);
        order.setCreatedAt(LocalDateTime.of(2026, 11, 10, 14, 0, 0));

        Customer customer = new Customer();
        customer.setCustomerId(1L);
        customer.setCustomerName("Nguyễn Văn A");
        customer.setPhone("0987654321");
        order.setCustomer(customer);

        Product product = new Product();
        product.setProductId(10L);
        product.setProductName("Áo thun Lauriel Basic");

        Size size = new Size();
        size.setSizeId(1L);
        size.setSizeName("L");

        Color color = new Color();
        color.setColorId(1L);
        color.setColorName("Trắng");

        ProductVariant variant = new ProductVariant();
        variant.setVariantId(201L);
        variant.setSku("AT-W-L");
        variant.setProduct(product);
        variant.setSize(size);
        variant.setColor(color);

        OrderDetail detail = new OrderDetail();
        detail.setId(new OrderDetailId(101L, 201L));
        detail.setOrder(order);
        detail.setProductVariant(variant);
        detail.setQuantity(2);
        detail.setUnitPrice(BigDecimal.valueOf(250000));
        detail.setSubtotal(BigDecimal.valueOf(500000));

        when(orderService.listForExport(any(), any(), any(), any(), any())).thenReturn(List.of(order));
        when(orderService.listDetailsForExport(any())).thenReturn(List.of(detail));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.exportOrders("Lauriel", OrderStatus.DELIVERED, null, null, null, out);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(2);

            Sheet ordersSheet = wb.getSheet("Orders");
            assertThat(ordersSheet).isNotNull();
            assertThat(ordersSheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("DANH SÁCH ĐƠN HÀNG");
            assertThat(ordersSheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Mã đơn hàng");
            assertThat(ordersSheet.getRow(4).getCell(0).getNumericCellValue()).isEqualTo(101.0);
            assertThat(ordersSheet.getRow(4).getCell(1).getStringCellValue()).contains("Nguyễn Văn A");
            assertThat(ordersSheet.getRow(4).getCell(3).getStringCellValue()).isEqualTo("Đã giao");
            assertThat(ordersSheet.getRow(4).getCell(7).getNumericCellValue()).isEqualTo(480000.0);

            Sheet detailsSheet = wb.getSheet("OrderDetails");
            assertThat(detailsSheet).isNotNull();
            assertThat(detailsSheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("CHI TIẾT MẶT HÀNG ĐƠN HÀNG");
            assertThat(detailsSheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Mã đơn hàng");
            assertThat(detailsSheet.getRow(4).getCell(0).getNumericCellValue()).isEqualTo(101.0);
            assertThat(detailsSheet.getRow(4).getCell(1).getStringCellValue()).isEqualTo("Áo thun Lauriel Basic");
            assertThat(detailsSheet.getRow(4).getCell(2).getStringCellValue()).isEqualTo("AT-W-L");
            assertThat(detailsSheet.getRow(4).getCell(5).getNumericCellValue()).isEqualTo(2.0);
        }
    }

    @Test
    @DisplayName("Xuất Excel Đơn hàng khi danh sách rỗng vẫn tạo file hợp lệ")
    void testExportOrdersEmpty() throws IOException {
        when(orderService.listForExport(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(orderService.listDetailsForExport(any())).thenReturn(List.of());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.exportOrders(null, null, null, null, null, out);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(2);
            Sheet ordersSheet = wb.getSheet("Orders");
            assertThat(ordersSheet.getRow(1).getCell(0).getStringCellValue()).contains("Tổng số: 0 đơn hàng");
        }
    }
}
