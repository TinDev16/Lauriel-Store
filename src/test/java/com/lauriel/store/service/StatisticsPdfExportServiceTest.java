package com.lauriel.store.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lauriel.store.controller.web.WebViewSupport;
import com.lauriel.store.dto.statistics.StatisticsReport;
import com.lauriel.store.entity.enums.OrderStatus;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class StatisticsPdfExportServiceTest {

    private StatisticsPdfExportService pdfExportService;

    @BeforeEach
    void setUp() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        templateEngine.setTemplateResolver(resolver);

        WebViewSupport viewSupport = new WebViewSupport();
        pdfExportService = new StatisticsPdfExportService(templateEngine, viewSupport);
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
            BigDecimal.valueOf(100000000),
            BigDecimal.valueOf(5000000),
            BigDecimal.valueOf(45000000),
            BigDecimal.valueOf(90000000),
            15,
            120,
            250,
            10,
            80,
            35,
            120,
            1500,
            12,
            3,
            BigDecimal.valueOf(30000000)
        );

        StatisticsReport.Sales sales = new StatisticsReport.Sales(
            150,
            135,
            15,
            BigDecimal.valueOf(45000000),
            BigDecimal.valueOf(333333.33),
            BigDecimal.valueOf(10.0)
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
            new StatisticsReport.TopProduct(1L, "Áo Polo Nam Lauriel Classic", 45, BigDecimal.valueOf(13500000)),
            new StatisticsReport.TopProduct(2L, "Quần Jean Slimfit Co Giãn", 30, BigDecimal.valueOf(15000000))
        );

        List<StatisticsReport.StockVariant> lowStock = List.of(
            new StatisticsReport.StockVariant(101L, "POLO-W-L", "Áo Polo Nam Lauriel Classic", "L", "Trắng", 3)
        );

        List<StatisticsReport.StockVariant> topStock = List.of(
            new StatisticsReport.StockVariant(102L, "JEAN-BL-32", "Quần Jean Slimfit Co Giãn", "32", "Đen", 120)
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
            new StatisticsReport.PromotionProduct(1L, "Áo Polo Nam Lauriel Classic", 10, BigDecimal.valueOf(200000))
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
    @DisplayName("Xuất PDF Báo cáo thống kê tổng quan tạo file PDF hợp lệ có header %PDF- và có trang")
    void exportReportPdf_Overview_ShouldGenerateValidPdf() throws IOException {
        StatisticsReport report = createSampleReport();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        pdfExportService.exportReportPdf(
            report,
            "overview",
            "THIS_MONTH",
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 30),
            "DAY",
            5,
            5,
            out
        );

        byte[] pdfBytes = out.toByteArray();
        assertThat(pdfBytes).isNotEmpty();
        assertThat(new String(pdfBytes, 0, 5)).isEqualTo("%PDF-");

        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    @DisplayName("Xuất PDF cho từng section (sales, products, customers, promotions, inventory)")
    void exportReportPdf_DifferentSections_ShouldAllSucceed() throws IOException {
        StatisticsReport report = createSampleReport();
        String[] sections = { "sales", "products", "customers", "promotions", "inventory" };

        for (String section : sections) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pdfExportService.exportReportPdf(
                report,
                section,
                "THIS_MONTH",
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30),
                "DAY",
                5,
                5,
                out
            );

            byte[] pdfBytes = out.toByteArray();
            assertThat(pdfBytes).isNotEmpty();
            assertThat(new String(pdfBytes, 0, 5)).isEqualTo("%PDF-");

            try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
                assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            }
        }
    }
}

