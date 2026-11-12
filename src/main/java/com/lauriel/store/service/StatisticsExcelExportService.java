package com.lauriel.store.service;

import com.lauriel.store.dto.statistics.StatisticsReport;
import com.lauriel.store.entity.enums.OrderStatus;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class StatisticsExcelExportService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void exportReport(StatisticsReport report, String section, OutputStream outputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Styles styles = createStyles(workbook);

            writeOverviewSheet(workbook, styles, report);
            writeRevenueSheet(workbook, styles, report);
            writeOrderStatisticsSheet(workbook, styles, report);
            writeProductStatisticsSheet(workbook, styles, report);
            writeInventorySheet(workbook, styles, report);
            writePurchaseSheet(workbook, styles, report);
            writeCustomerStatisticsSheet(workbook, styles, report);
            writePromotionStatisticsSheet(workbook, styles, report);

            workbook.write(outputStream);
        }
    }

    // -------------------------------------------------------------
    // Sheet 1: Overview
    // -------------------------------------------------------------
    private void writeOverviewSheet(Workbook workbook, Styles styles, StatisticsReport report) {
        Sheet sheet = workbook.createSheet("Overview");
        sheet.setDisplayGridlines(true);

        createSheetTitle(sheet, styles, "TỔNG QUAN CHỈ SỐ KINH DOANH", report);

        String[] headers = { "Chỉ số kinh doanh", "Giá trị", "Ghi chú / Đơn vị" };
        createHeaders(sheet, styles, headers, 3);

        StatisticsReport.Kpis kpis = report.kpis();
        StatisticsReport.Sales sales = report.sales();

        Object[][] data = {
            { "Tổng doanh thu", kpis != null ? kpis.revenueAllTime() : BigDecimal.ZERO, "Doanh thu toàn thời gian (đã giao)", true, false },
            { "Doanh thu hôm nay", kpis != null ? kpis.revenueToday() : BigDecimal.ZERO, "Doanh thu ghi nhận trong ngày", true, false },
            { "Doanh thu tháng này", kpis != null ? kpis.revenueMonth() : BigDecimal.ZERO, "Doanh thu trong tháng hiện tại", true, false },
            { "Doanh thu năm nay", kpis != null ? kpis.revenueYear() : BigDecimal.ZERO, "Doanh thu trong năm hiện tại", true, false },
            { "Tổng đơn hàng", sales != null ? sales.totalOrders() : 0L, "Tổng đơn phát sinh trong kỳ", false, true },
            { "Đơn hàng đã giao", sales != null ? sales.delivered() : 0L, "Đơn hàng hoàn tất trong kỳ", false, true },
            { "Đơn hàng đã hủy", sales != null ? sales.cancelled() : 0L, "Đơn hàng hủy trong kỳ", false, true },
            { "Tổng khách hàng", kpis != null ? kpis.customers() : 0L, "Tổng số khách hàng đăng ký", false, true },
            { "Tổng sản phẩm đang bán", kpis != null ? kpis.activeProducts() : 0L, "Sản phẩm ở trạng thái hoạt động", false, true },
            { "Tổng tồn kho", kpis != null ? kpis.totalStock() : 0L, "Tổng số lượng sản phẩm trong kho", false, true }
        };

        int r = 4;
        for (Object[] rowData : data) {
            Row row = sheet.createRow(r++);
            row.setHeightInPoints(22);

            // 0: Chỉ số
            Cell c0 = row.createCell(0);
            c0.setCellValue((String) rowData[0]);
            c0.setCellStyle(styles.dataLeftBold);

            // 1: Giá trị
            Cell c1 = row.createCell(1);
            boolean isCurrency = (boolean) rowData[3];
            boolean isNumber = (boolean) rowData[4];

            if (isCurrency) {
                BigDecimal val = (BigDecimal) rowData[1];
                c1.setCellValue(val != null ? val.doubleValue() : 0.0);
                c1.setCellStyle(styles.currency);
            } else if (isNumber) {
                long val = (long) rowData[1];
                c1.setCellValue(val);
                c1.setCellStyle(styles.dataNumber);
            } else {
                c1.setCellValue(String.valueOf(rowData[1]));
                c1.setCellStyle(styles.dataCenter);
            }

            // 2: Ghi chú
            Cell c2 = row.createCell(2);
            c2.setCellValue((String) rowData[2]);
            c2.setCellStyle(styles.dataLeft);
        }

        finalizeSheet(sheet, headers.length, r);
    }

    // -------------------------------------------------------------
    // Sheet 2: Revenue
    // -------------------------------------------------------------
    private void writeRevenueSheet(Workbook workbook, Styles styles, StatisticsReport report) {
        Sheet sheet = workbook.createSheet("Revenue");
        sheet.setDisplayGridlines(true);

        createSheetTitle(sheet, styles, "BÁO CÁO DOANH THU THEO KỲ", report);

        String[] headers = { "Ngày", "Doanh thu", "Số lượng đơn hàng" };
        createHeaders(sheet, styles, headers, 3);

        int r = 4;
        if (report.revenue() != null) {
            for (StatisticsReport.RevenuePoint pt : report.revenue()) {
                Row row = sheet.createRow(r++);
                row.setHeightInPoints(20);

                // 0: Ngày
                Cell c0 = row.createCell(0);
                c0.setCellValue(pt.label());
                c0.setCellStyle(styles.dataCenter);

                // 1: Doanh thu
                Cell c1 = row.createCell(1);
                c1.setCellValue(pt.revenue() != null ? pt.revenue().doubleValue() : 0.0);
                c1.setCellStyle(styles.currency);

                // 2: Số lượng đơn hàng
                Cell c2 = row.createCell(2);
                c2.setCellValue(pt.orderCount());
                c2.setCellStyle(styles.dataNumber);
            }
        }

        finalizeSheet(sheet, headers.length, r);
    }

    // -------------------------------------------------------------
    // Sheet 3: OrderStatistics
    // -------------------------------------------------------------
    private void writeOrderStatisticsSheet(Workbook workbook, Styles styles, StatisticsReport report) {
        Sheet sheet = workbook.createSheet("OrderStatistics");
        sheet.setDisplayGridlines(true);

        createSheetTitle(sheet, styles, "CƠ CẤU TRẠNG THÁI ĐƠN HÀNG", report);

        String[] headers = { "Trạng thái đơn", "Số lượng", "Tỷ lệ phần trăm" };
        createHeaders(sheet, styles, headers, 3);

        int r = 4;
        if (report.statuses() != null) {
            for (StatisticsReport.StatusPoint sp : report.statuses()) {
                Row row = sheet.createRow(r++);
                row.setHeightInPoints(20);

                // 0: Trạng thái đơn
                Cell c0 = row.createCell(0);
                c0.setCellValue(formatOrderStatus(sp.status()));
                c0.setCellStyle(styles.dataLeft);

                // 1: Số lượng
                Cell c1 = row.createCell(1);
                c1.setCellValue(sp.count());
                c1.setCellStyle(styles.dataNumber);

                // 2: Tỷ lệ phần trăm
                Cell c2 = row.createCell(2);
                if (sp.percentage() != null) {
                    c2.setCellValue(sp.percentage().doubleValue() / 100.0);
                    c2.setCellStyle(styles.percentage);
                } else {
                    c2.setCellValue(0.0);
                    c2.setCellStyle(styles.percentage);
                }
            }
        }

        finalizeSheet(sheet, headers.length, r);
    }

    // -------------------------------------------------------------
    // Sheet 4: ProductStatistics
    // -------------------------------------------------------------
    private void writeProductStatisticsSheet(Workbook workbook, Styles styles, StatisticsReport report) {
        Sheet sheet = workbook.createSheet("ProductStatistics");
        sheet.setDisplayGridlines(true);

        createSheetTitle(sheet, styles, "THỐNG KÊ HIỆU QUẢ SẢN PHẨM", report);

        String[] headers = { "Sản phẩm", "Số lượng bán", "Doanh thu" };
        createHeaders(sheet, styles, headers, 3);

        int r = 4;
        List<StatisticsReport.TopProduct> products = report.topProducts() != null && !report.topProducts().isEmpty()
            ? report.topProducts()
            : report.topProductsByRevenue();

        if (products != null) {
            for (StatisticsReport.TopProduct tp : products) {
                Row row = sheet.createRow(r++);
                row.setHeightInPoints(20);

                // 0: Sản phẩm
                Cell c0 = row.createCell(0);
                c0.setCellValue(tp.productName() != null ? tp.productName() : "—");
                c0.setCellStyle(styles.dataLeft);

                // 1: Số lượng bán
                Cell c1 = row.createCell(1);
                c1.setCellValue(tp.quantity());
                c1.setCellStyle(styles.dataNumber);

                // 2: Doanh thu
                Cell c2 = row.createCell(2);
                c2.setCellValue(tp.revenue() != null ? tp.revenue().doubleValue() : 0.0);
                c2.setCellStyle(styles.currency);
            }
        }

        finalizeSheet(sheet, headers.length, r);
    }

    // -------------------------------------------------------------
    // Sheet 5: Inventory
    // -------------------------------------------------------------
    private void writeInventorySheet(Workbook workbook, Styles styles, StatisticsReport report) {
        Sheet sheet = workbook.createSheet("Inventory");
        sheet.setDisplayGridlines(true);

        createSheetTitle(sheet, styles, "CẢNH BÁO TỒN KHO THEO THUỘC TÍNH", report);

        String[] headers = { "Sản phẩm", "SKU", "Size", "Color", "Số lượng tồn" };
        createHeaders(sheet, styles, headers, 3);

        // Combine lowStock and topStock without duplicate variantIds
        Map<Long, StatisticsReport.StockVariant> variantMap = new LinkedHashMap<>();
        if (report.lowStock() != null) {
            for (StatisticsReport.StockVariant v : report.lowStock()) {
                variantMap.put(v.variantId(), v);
            }
        }
        if (report.topStock() != null) {
            for (StatisticsReport.StockVariant v : report.topStock()) {
                variantMap.putIfAbsent(v.variantId(), v);
            }
        }

        int r = 4;
        for (StatisticsReport.StockVariant v : variantMap.values()) {
            Row row = sheet.createRow(r++);
            row.setHeightInPoints(20);

            // 0: Sản phẩm
            Cell c0 = row.createCell(0);
            c0.setCellValue(v.productName() != null ? v.productName() : "—");
            c0.setCellStyle(styles.dataLeft);

            // 1: SKU
            Cell c1 = row.createCell(1);
            c1.setCellValue(v.sku() != null ? v.sku() : "—");
            c1.setCellStyle(styles.dataLeft);

            // 2: Size
            Cell c2 = row.createCell(2);
            c2.setCellValue(v.size() != null ? v.size() : "—");
            c2.setCellStyle(styles.dataCenter);

            // 3: Color
            Cell c3 = row.createCell(3);
            c3.setCellValue(v.color() != null ? v.color() : "—");
            c3.setCellStyle(styles.dataCenter);

            // 4: Số lượng tồn
            Cell c4 = row.createCell(4);
            c4.setCellValue(v.quantity());
            c4.setCellStyle(styles.dataNumber);
        }

        finalizeSheet(sheet, headers.length, r);
    }

    // -------------------------------------------------------------
    // Sheet 6: Purchase
    // -------------------------------------------------------------
    private void writePurchaseSheet(Workbook workbook, Styles styles, StatisticsReport report) {
        Sheet sheet = workbook.createSheet("Purchase");
        sheet.setDisplayGridlines(true);

        createSheetTitle(sheet, styles, "THỐNG KÊ NHẬP HÀNG TỪ NHÀ CUNG CẤP", report);

        String[] headers = { "Nhà cung cấp", "Số phiếu nhập", "Giá trị nhập", "Số lượng nhập" };
        createHeaders(sheet, styles, headers, 3);

        int r = 4;
        if (report.topSuppliers() != null) {
            for (StatisticsReport.Supplier s : report.topSuppliers()) {
                Row row = sheet.createRow(r++);
                row.setHeightInPoints(20);

                // 0: Nhà cung cấp
                Cell c0 = row.createCell(0);
                c0.setCellValue(s.supplierName() != null ? s.supplierName() : "—");
                c0.setCellStyle(styles.dataLeft);

                // 1: Số phiếu nhập
                Cell c1 = row.createCell(1);
                c1.setCellValue(s.receiptCount());
                c1.setCellStyle(styles.dataNumber);

                // 2: Giá trị nhập
                Cell c2 = row.createCell(2);
                c2.setCellValue(s.purchaseAmount() != null ? s.purchaseAmount().doubleValue() : 0.0);
                c2.setCellStyle(styles.currency);

                // 3: Số lượng nhập
                Cell c3 = row.createCell(3);
                c3.setCellValue("—");
                c3.setCellStyle(styles.dataCenter);
            }
        }

        // Summary row for total purchases across period
        if (report.purchases() != null && report.purchases().receipts() > 0) {
            Row summaryRow = sheet.createRow(r++);
            summaryRow.setHeightInPoints(22);

            Cell sc0 = summaryRow.createCell(0);
            sc0.setCellValue("TỔNG CỘNG TOÀN KỲ");
            sc0.setCellStyle(styles.dataLeftBold);

            Cell sc1 = summaryRow.createCell(1);
            sc1.setCellValue(report.purchases().receipts());
            sc1.setCellStyle(styles.dataNumberBold);

            Cell sc2 = summaryRow.createCell(2);
            sc2.setCellValue(report.purchases().amount() != null ? report.purchases().amount().doubleValue() : 0.0);
            sc2.setCellStyle(styles.currencyBold);

            Cell sc3 = summaryRow.createCell(3);
            sc3.setCellValue(report.purchases().quantity());
            sc3.setCellStyle(styles.dataNumberBold);
        }

        finalizeSheet(sheet, headers.length, r);
    }

    // -------------------------------------------------------------
    // Sheet 7: CustomerStatistics
    // -------------------------------------------------------------
    private void writeCustomerStatisticsSheet(Workbook workbook, Styles styles, StatisticsReport report) {
        Sheet sheet = workbook.createSheet("CustomerStatistics");
        sheet.setDisplayGridlines(true);

        createSheetTitle(sheet, styles, "THỐNG KÊ CHI TIÊU KHÁCH HÀNG", report);

        String[] headers = { "Khách hàng", "Số đơn", "Tổng tiền đã chi" };
        createHeaders(sheet, styles, headers, 3);

        int r = 4;
        if (report.customerSpending() != null && !report.customerSpending().isEmpty()) {
            for (StatisticsReport.CustomerSpendingSummary c : report.customerSpending()) {
                Row row = sheet.createRow(r++);
                row.setHeightInPoints(20);

                // 0: Khách hàng
                Cell c0 = row.createCell(0);
                c0.setCellValue(c.customerName() != null ? c.customerName() : "—");
                c0.setCellStyle(styles.dataLeft);

                // 1: Số đơn (ưu tiên deliveredOrders hoặc totalOrders)
                Cell c1 = row.createCell(1);
                c1.setCellValue(c.deliveredOrders() > 0 ? c.deliveredOrders() : c.totalOrders());
                c1.setCellStyle(styles.dataNumber);

                // 2: Tổng tiền đã chi
                Cell c2 = row.createCell(2);
                c2.setCellValue(c.totalSpent() != null ? c.totalSpent().doubleValue() : 0.0);
                c2.setCellStyle(styles.currency);
            }
        } else if (report.topCustomers() != null) {
            for (StatisticsReport.TopCustomer c : report.topCustomers()) {
                Row row = sheet.createRow(r++);
                row.setHeightInPoints(20);

                Cell c0 = row.createCell(0);
                c0.setCellValue(c.customerName() != null ? c.customerName() : "—");
                c0.setCellStyle(styles.dataLeft);

                Cell c1 = row.createCell(1);
                c1.setCellValue(c.orderCount());
                c1.setCellStyle(styles.dataNumber);

                Cell c2 = row.createCell(2);
                c2.setCellValue(c.totalSpent() != null ? c.totalSpent().doubleValue() : 0.0);
                c2.setCellStyle(styles.currency);
            }
        }

        finalizeSheet(sheet, headers.length, r);
    }

    // -------------------------------------------------------------
    // Sheet 8: PromotionStatistics
    // -------------------------------------------------------------
    private void writePromotionStatisticsSheet(Workbook workbook, Styles styles, StatisticsReport report) {
        Sheet sheet = workbook.createSheet("PromotionStatistics");
        sheet.setDisplayGridlines(true);

        createSheetTitle(sheet, styles, "HIỆU QUẢ VOUCHER VÀ CHƯƠNG TRÌNH KHUYẾN MÃI", report);

        String[] headers = { "Chương trình", "Số lượt sử dụng", "Số tiền giảm" };
        createHeaders(sheet, styles, headers, 3);

        int r = 4;

        // 1. Top Vouchers
        if (report.topVouchersByUses() != null) {
            for (StatisticsReport.Voucher v : report.topVouchersByUses()) {
                Row row = sheet.createRow(r++);
                row.setHeightInPoints(20);

                Cell c0 = row.createCell(0);
                c0.setCellValue("Voucher: " + (v.code() != null ? v.code() : "—"));
                c0.setCellStyle(styles.dataLeft);

                Cell c1 = row.createCell(1);
                c1.setCellValue(v.orderCount());
                c1.setCellStyle(styles.dataNumber);

                Cell c2 = row.createCell(2);
                c2.setCellValue(v.discount() != null ? v.discount().doubleValue() : 0.0);
                c2.setCellStyle(styles.currency);
            }
        }

        // 2. Top Order Promotions
        if (report.topOrderPromotionsByUses() != null) {
            for (StatisticsReport.OrderPromotion op : report.topOrderPromotionsByUses()) {
                Row row = sheet.createRow(r++);
                row.setHeightInPoints(20);

                Cell c0 = row.createCell(0);
                c0.setCellValue("Ưu đãi đơn hàng: " + (op.promotionName() != null ? op.promotionName() : "—"));
                c0.setCellStyle(styles.dataLeft);

                Cell c1 = row.createCell(1);
                c1.setCellValue(op.orderCount());
                c1.setCellStyle(styles.dataNumber);

                Cell c2 = row.createCell(2);
                c2.setCellValue(op.discount() != null ? op.discount().doubleValue() : 0.0);
                c2.setCellStyle(styles.currency);
            }
        }

        // 3. Top Promotion Products
        if (report.topPromotionProducts() != null) {
            for (StatisticsReport.PromotionProduct pp : report.topPromotionProducts()) {
                Row row = sheet.createRow(r++);
                row.setHeightInPoints(20);

                Cell c0 = row.createCell(0);
                c0.setCellValue("Khuyến mãi sản phẩm: " + (pp.productName() != null ? pp.productName() : "—"));
                c0.setCellStyle(styles.dataLeft);

                Cell c1 = row.createCell(1);
                c1.setCellValue(pp.quantity());
                c1.setCellStyle(styles.dataNumber);

                Cell c2 = row.createCell(2);
                c2.setCellValue(pp.discount() != null ? pp.discount().doubleValue() : 0.0);
                c2.setCellStyle(styles.currency);
            }
        }

        finalizeSheet(sheet, headers.length, r);
    }

    // -------------------------------------------------------------
    // Helper Layout & Styling Methods
    // -------------------------------------------------------------
    private void createSheetTitle(Sheet sheet, Styles styles, String title, StatisticsReport report) {
        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(28);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(styles.title);

        Row subRow = sheet.createRow(1);
        subRow.setHeightInPoints(18);
        Cell subCell = subRow.createCell(0);

        String period = "Toàn thời gian";
        if (report.range() != null && report.range().fromDate() != null && report.range().toDate() != null) {
            period = report.range().fromDate().format(DATE_FORMATTER) + " – " + report.range().toDate().format(DATE_FORMATTER);
            if (report.range().preset() != null && !report.range().preset().isBlank()) {
                period += " (" + report.range().preset() + ")";
            }
        }

        subCell.setCellValue(
            "Thời gian xuất: " + LocalDateTime.now().format(DATE_TIME_FORMATTER) +
            " | Kỳ báo cáo: " + period
        );
        subCell.setCellStyle(styles.subtitle);
    }

    private void createHeaders(Sheet sheet, Styles styles, String[] headers, int rowIdx) {
        Row headerRow = sheet.createRow(rowIdx);
        headerRow.setHeightInPoints(26);
        for (int i = 0; i < headers.length; i++) {
            Cell c = headerRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(styles.header);
        }
    }

    private void finalizeSheet(Sheet sheet, int colCount, int lastDataRow) {
        sheet.createFreezePane(0, 4);
        int lastRow = Math.max(4, lastDataRow - 1);
        sheet.setAutoFilter(new CellRangeAddress(3, lastRow, 0, colCount - 1));

        for (int i = 0; i < colCount; i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i) + 1200;
            sheet.setColumnWidth(i, Math.min(Math.max(width, 3600), 15000));
        }
    }

    private String formatOrderStatus(OrderStatus status) {
        if (status == null) return "—";
        return switch (status) {
            case PENDING -> "Chờ xác nhận";
            case CONFIRMED -> "Đã xác nhận";
            case PROCESSING -> "Đang xử lý";
            case SHIPPING -> "Đang giao";
            case DELIVERED -> "Đã giao";
            case CANCELLED -> "Đã hủy";
            case RETURNED -> "Đã trả hàng";
        };
    }

    private Styles createStyles(Workbook workbook) {
        DataFormat dataFormat = workbook.createDataFormat();

        Font titleFont = workbook.createFont();
        titleFont.setFontName("Calibri");
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setBold(true);
        titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());

        CellStyle title = workbook.createCellStyle();
        title.setFont(titleFont);
        title.setAlignment(HorizontalAlignment.LEFT);
        title.setVerticalAlignment(VerticalAlignment.CENTER);

        Font subFont = workbook.createFont();
        subFont.setFontName("Calibri");
        subFont.setFontHeightInPoints((short) 10);
        subFont.setItalic(true);
        subFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

        CellStyle subtitle = workbook.createCellStyle();
        subtitle.setFont(subFont);
        subtitle.setAlignment(HorizontalAlignment.LEFT);
        subtitle.setVerticalAlignment(VerticalAlignment.CENTER);

        Font headerFont = workbook.createFont();
        headerFont.setFontName("Calibri");
        headerFont.setFontHeightInPoints((short) 11);
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        CellStyle header = workbook.createCellStyle();
        header.setFont(headerFont);
        header.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header.setAlignment(HorizontalAlignment.CENTER);
        header.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(header);

        Font dataFont = workbook.createFont();
        dataFont.setFontName("Calibri");
        dataFont.setFontHeightInPoints((short) 10);

        Font boldDataFont = workbook.createFont();
        boldDataFont.setFontName("Calibri");
        boldDataFont.setFontHeightInPoints((short) 10);
        boldDataFont.setBold(true);

        CellStyle dataLeft = workbook.createCellStyle();
        dataLeft.setFont(dataFont);
        dataLeft.setAlignment(HorizontalAlignment.LEFT);
        dataLeft.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(dataLeft);

        CellStyle dataLeftBold = workbook.createCellStyle();
        dataLeftBold.cloneStyleFrom(dataLeft);
        dataLeftBold.setFont(boldDataFont);

        CellStyle dataCenter = workbook.createCellStyle();
        dataCenter.cloneStyleFrom(dataLeft);
        dataCenter.setAlignment(HorizontalAlignment.CENTER);

        CellStyle currency = workbook.createCellStyle();
        currency.cloneStyleFrom(dataLeft);
        currency.setAlignment(HorizontalAlignment.RIGHT);
        currency.setDataFormat(dataFormat.getFormat("#,##0 \"₫\""));

        CellStyle currencyBold = workbook.createCellStyle();
        currencyBold.cloneStyleFrom(currency);
        currencyBold.setFont(boldDataFont);

        CellStyle dataNumber = workbook.createCellStyle();
        dataNumber.cloneStyleFrom(dataLeft);
        dataNumber.setAlignment(HorizontalAlignment.RIGHT);
        dataNumber.setDataFormat(dataFormat.getFormat("#,##0"));

        CellStyle dataNumberBold = workbook.createCellStyle();
        dataNumberBold.cloneStyleFrom(dataNumber);
        dataNumberBold.setFont(boldDataFont);

        CellStyle percentage = workbook.createCellStyle();
        percentage.cloneStyleFrom(dataLeft);
        percentage.setAlignment(HorizontalAlignment.RIGHT);
        percentage.setDataFormat(dataFormat.getFormat("0.00%"));

        return new Styles(
            title,
            subtitle,
            header,
            dataLeft,
            dataLeftBold,
            dataCenter,
            currency,
            currencyBold,
            dataNumber,
            dataNumberBold,
            percentage
        );
    }

    private void setBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
    }

    private record Styles(
        CellStyle title,
        CellStyle subtitle,
        CellStyle header,
        CellStyle dataLeft,
        CellStyle dataLeftBold,
        CellStyle dataCenter,
        CellStyle currency,
        CellStyle currencyBold,
        CellStyle dataNumber,
        CellStyle dataNumberBold,
        CellStyle percentage
    ) {}
}

