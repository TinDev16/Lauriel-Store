package com.lauriel.store.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lauriel.store.dto.statistics.StatisticsReport;
import com.lauriel.store.entity.enums.OrderStatus;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StatisticsExcelExportServiceTest {

    private StatisticsExcelExportService exportService;

    @BeforeEach
    void setUp() {
        exportService = new StatisticsExcelExportService();
    }

    private StatisticsReport createSampleReport() {
        StatisticsReport.Range range = new StatisticsReport.Range(
            "THIS_MONTH",
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 30),
            "DAY",
            5,
            5
        );

        StatisticsReport.Kpis kpis = new StatisticsReport.Kpis(
            BigDecimal.valueOf(100000000), // revenueAllTime
            BigDecimal.valueOf(5000000),   // revenueToday
            BigDecimal.valueOf(45000000),  // revenueMonth
            BigDecimal.valueOf(90000000),  // revenueYear
            15,                            // ordersToday
            120,                           // ordersMonth
            250,                           // deliveredAllTime
            10,                            // cancelledAllTime
            80,                            // customers
            35,                            // activeProducts
            120,                           // activeVariants
            1500,                          // totalStock
            12,                            // lowStock
            3,                             // draftReceipts
            BigDecimal.valueOf(30000000)   // purchaseMonth
        );

        StatisticsReport.Sales sales = new StatisticsReport.Sales(
            150,                           // totalOrders
            135,                           // delivered
            15,                            // cancelled
            BigDecimal.valueOf(45000000),  // revenue
            BigDecimal.valueOf(333333.33), // averageOrderValue
            BigDecimal.valueOf(10.0)       // cancelRate
        );

        List<StatisticsReport.RevenuePoint> revenue = List.of(
            new StatisticsReport.RevenuePoint("01/11", BigDecimal.valueOf(1500000), 5),
            new StatisticsReport.RevenuePoint("02/11", BigDecimal.valueOf(2200000), 8)
        );

        List<StatisticsReport.StatusPoint> statuses = List.of(
            new StatisticsReport.StatusPoint(OrderStatus.DELIVERED, 135, BigDecimal.valueOf(90.0)),
            new StatisticsReport.StatusPoint(OrderStatus.CANCELLED, 15, BigDecimal.valueOf(10.0))
        );

        List<StatisticsReport.TopProduct> topProducts = List.of(
            new StatisticsReport.TopProduct(1L, "Áo Polo Nam", 45, BigDecimal.valueOf(13500000)),
            new StatisticsReport.TopProduct(2L, "Quần Jean Slimfit", 30, BigDecimal.valueOf(15000000))
        );

        List<StatisticsReport.StockVariant> lowStock = List.of(
            new StatisticsReport.StockVariant(101L, "POLO-W-L", "Áo Polo Nam", "L", "Trắng", 3)
        );

        List<StatisticsReport.StockVariant> topStock = List.of(
            new StatisticsReport.StockVariant(102L, "JEAN-BL-32", "Quần Jean Slimfit", "32", "Đen", 120)
        );

        StatisticsReport.Purchase purchases = new StatisticsReport.Purchase(5, BigDecimal.valueOf(25000000), 300);

        List<StatisticsReport.Supplier> topSuppliers = List.of(
            new StatisticsReport.Supplier(10L, "May Mặc Sài Gòn", 3, BigDecimal.valueOf(15000000))
        );

        StatisticsReport.Discounts discounts = new StatisticsReport.Discounts(
            BigDecimal.valueOf(1000000),
            BigDecimal.valueOf(500000),
            BigDecimal.valueOf(700000),
            BigDecimal.valueOf(2200000),
            20,
            15,
            10
        );

        List<StatisticsReport.Voucher> topVouchers = List.of(
            new StatisticsReport.Voucher(1L, "GIAM10", 12, BigDecimal.valueOf(600000))
        );

        List<StatisticsReport.OrderPromotion> topOrderPromotions = List.of(
            new StatisticsReport.OrderPromotion(1L, "Ưu đãi đơn từ 500k", 8, BigDecimal.valueOf(400000))
        );

        List<StatisticsReport.PromotionProduct> topPromotionProducts = List.of(
            new StatisticsReport.PromotionProduct(1L, "Áo Polo Nam", 10, BigDecimal.valueOf(200000))
        );

        List<StatisticsReport.TopCustomer> topCustomers = List.of(
            new StatisticsReport.TopCustomer(5L, "Trần Văn An", 6, BigDecimal.valueOf(3200000))
        );

        List<StatisticsReport.CustomerSpendingSummary> customerSpending = List.of(
            new StatisticsReport.CustomerSpendingSummary(
                5L,
                "Trần Văn An",
                "0909123456",
                "an@gmail.com",
                7,
                6,
                1,
                BigDecimal.valueOf(3200000),
                BigDecimal.valueOf(533333.33)
            )
        );

        return new StatisticsReport(
            range,
            kpis,
            sales,
            revenue,
            statuses,
            topProducts,
            topProducts,
            List.of(),
            topCustomers,
            topCustomers,
            new StatisticsReport.Inventory(1500, 120, 12, 2, BigDecimal.valueOf(450000000)),
            lowStock,
            topStock,
            purchases,
            List.of(),
            topSuppliers,
            discounts,
            topVouchers,
            topVouchers,
            topPromotionProducts,
            topOrderPromotions,
            topOrderPromotions,
            75,
            60,
            15,
            List.of(),
            List.of(),
            new StatisticsReport.Engagement(5, 8, 12, 20),
            List.of(),
            List.of(),
            customerSpending
        );
    }

    @Test
    @DisplayName("Xuất Excel Báo cáo thống kê tạo đầy đủ 8 sheet đúng định dạng và số liệu")
    void testExportReportSuccess() throws IOException {
        StatisticsReport report = createSampleReport();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        exportService.exportReport(report, "overview", out);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(8);

            // Sheet 1: Overview
            Sheet s1 = wb.getSheet("Overview");
            assertThat(s1).isNotNull();
            assertThat(s1.getRow(0).getCell(0).getStringCellValue()).isEqualTo("TỔNG QUAN CHỈ SỐ KINH DOANH");
            assertThat(s1.getRow(4).getCell(0).getStringCellValue()).isEqualTo("Tổng doanh thu");
            assertThat(s1.getRow(4).getCell(1).getNumericCellValue()).isEqualTo(100000000.0);
            assertThat(s1.getRow(8).getCell(0).getStringCellValue()).isEqualTo("Tổng đơn hàng");
            assertThat(s1.getRow(8).getCell(1).getNumericCellValue()).isEqualTo(150.0);

            // Sheet 2: Revenue
            Sheet s2 = wb.getSheet("Revenue");
            assertThat(s2).isNotNull();
            assertThat(s2.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Ngày");
            assertThat(s2.getRow(4).getCell(0).getStringCellValue()).isEqualTo("01/11");
            assertThat(s2.getRow(4).getCell(1).getNumericCellValue()).isEqualTo(1500000.0);

            // Sheet 3: OrderStatistics
            Sheet s3 = wb.getSheet("OrderStatistics");
            assertThat(s3).isNotNull();
            assertThat(s3.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Trạng thái đơn");
            assertThat(s3.getRow(4).getCell(0).getStringCellValue()).isEqualTo("Đã giao");
            assertThat(s3.getRow(4).getCell(1).getNumericCellValue()).isEqualTo(135.0);

            // Sheet 4: ProductStatistics
            Sheet s4 = wb.getSheet("ProductStatistics");
            assertThat(s4).isNotNull();
            assertThat(s4.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Sản phẩm");
            assertThat(s4.getRow(4).getCell(0).getStringCellValue()).isEqualTo("Áo Polo Nam");
            assertThat(s4.getRow(4).getCell(1).getNumericCellValue()).isEqualTo(45.0);

            // Sheet 5: Inventory
            Sheet s5 = wb.getSheet("Inventory");
            assertThat(s5).isNotNull();
            assertThat(s5.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Sản phẩm");
            assertThat(s5.getRow(4).getCell(0).getStringCellValue()).isEqualTo("Áo Polo Nam");
            assertThat(s5.getRow(4).getCell(1).getStringCellValue()).isEqualTo("POLO-W-L");

            // Sheet 6: Purchase
            Sheet s6 = wb.getSheet("Purchase");
            assertThat(s6).isNotNull();
            assertThat(s6.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Nhà cung cấp");
            assertThat(s6.getRow(4).getCell(0).getStringCellValue()).isEqualTo("May Mặc Sài Gòn");
            assertThat(s6.getRow(4).getCell(1).getNumericCellValue()).isEqualTo(3.0);
            assertThat(s6.getRow(4).getCell(2).getNumericCellValue()).isEqualTo(15000000.0);

            // Sheet 7: CustomerStatistics
            Sheet s7 = wb.getSheet("CustomerStatistics");
            assertThat(s7).isNotNull();
            assertThat(s7.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Khách hàng");
            assertThat(s7.getRow(4).getCell(0).getStringCellValue()).isEqualTo("Trần Văn An");
            assertThat(s7.getRow(4).getCell(2).getNumericCellValue()).isEqualTo(3200000.0);

            // Sheet 8: PromotionStatistics
            Sheet s8 = wb.getSheet("PromotionStatistics");
            assertThat(s8).isNotNull();
            assertThat(s8.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Chương trình");
            assertThat(s8.getRow(4).getCell(0).getStringCellValue()).isEqualTo("Voucher: GIAM10");
            assertThat(s8.getRow(4).getCell(1).getNumericCellValue()).isEqualTo(12.0);
            assertThat(s8.getRow(4).getCell(2).getNumericCellValue()).isEqualTo(600000.0);
        }
    }

    @Test
    @DisplayName("Xuất Excel Báo cáo thống kê khi dữ liệu rỗng không ném ngoại lệ")
    void testExportReportEmpty() throws IOException {
        StatisticsReport.Range range = new StatisticsReport.Range("TODAY", null, null, "DAY", 5, 5);
        StatisticsReport report = new StatisticsReport(
            range,
            null,
            null,
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            null,
            List.of(),
            List.of(),
            null,
            List.of(),
            List.of(),
            null,
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            0,
            0,
            0,
            List.of(),
            List.of(),
            null,
            List.of(),
            List.of(),
            List.of()
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.exportReport(report, null, out);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(8);
        }
    }
}

