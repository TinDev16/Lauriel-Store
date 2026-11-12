package com.lauriel.store.service;

import com.lauriel.store.entity.Order;
import com.lauriel.store.entity.OrderDetail;
import com.lauriel.store.entity.enums.OrderStatus;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderExcelExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final OrderService orderService;

    public OrderExcelExportService(OrderService orderService) {
        this.orderService = orderService;
    }

    @Transactional(readOnly = true)
    public void exportOrders(
        String keyword,
        OrderStatus status,
        LocalDateTime from,
        LocalDateTime to,
        Long customerId,
        OutputStream outputStream
    ) throws IOException {
        List<Order> orderList = orderService.listForExport(keyword, status, from, to, customerId);
        List<Long> orderIds = orderList.stream().map(Order::getOrderId).toList();
        List<OrderDetail> detailList = orderService.listDetailsForExport(orderIds);

        try (Workbook workbook = new XSSFWorkbook()) {
            Styles styles = createStyles(workbook);

            writeOrdersSheet(workbook, styles, orderList, keyword, status, from, to, customerId);
            writeOrderDetailsSheet(workbook, styles, detailList);

            workbook.write(outputStream);
        }
    }

    private void writeOrdersSheet(
        Workbook workbook,
        Styles styles,
        List<Order> orders,
        String keyword,
        OrderStatus status,
        LocalDateTime from,
        LocalDateTime to,
        Long customerId
    ) {
        Sheet sheet = workbook.createSheet("Orders");
        sheet.setDisplayGridlines(true);

        // Row 0: Title
        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(28);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("DANH SÁCH ĐƠN HÀNG");
        titleCell.setCellStyle(styles.title);

        // Row 1: Subtitle
        Row subRow = sheet.createRow(1);
        subRow.setHeightInPoints(18);
        Cell subCell = subRow.createCell(0);
        subCell.setCellValue(
            "Thời gian xuất: " + LocalDateTime.now().format(DATE_FORMATTER) +
            " | Tổng số: " + orders.size() + " đơn hàng" +
            " | Điều kiện lọc: " + buildFilterInfo(keyword, status, from, to, customerId)
        );
        subCell.setCellStyle(styles.subtitle);

        // Row 3: Headers
        String[] headers = {
            "Mã đơn hàng",
            "Khách hàng",
            "Ngày đặt hàng",
            "Trạng thái",
            "Tổng tiền",
            "Giảm giá",
            "Phí vận chuyển",
            "Thành tiền cuối cùng",
            "Kênh bán hàng"
        };
        Row headerRow = sheet.createRow(3);
        headerRow.setHeightInPoints(26);
        for (int i = 0; i < headers.length; i++) {
            Cell c = headerRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(styles.header);
        }

        int r = 4;
        for (Order o : orders) {
            Row row = sheet.createRow(r++);
            row.setHeightInPoints(20);

            // 0: Mã đơn hàng
            Cell c0 = row.createCell(0);
            c0.setCellValue(o.getOrderId());
            c0.setCellStyle(styles.dataCenter);

            // 1: Khách hàng
            Cell c1 = row.createCell(1);
            String custName = o.getCustomer() != null ? o.getCustomer().getCustomerName() : "Khách vãng lai";
            c1.setCellValue(custName);
            c1.setCellStyle(styles.dataLeft);

            // 2: Ngày đặt hàng
            Cell c2 = row.createCell(2);
            if (o.getCreatedAt() != null) {
                c2.setCellValue(o.getCreatedAt());
                c2.setCellStyle(styles.dataDate);
            } else {
                c2.setCellValue("—");
                c2.setCellStyle(styles.dataCenter);
            }

            // 3: Trạng thái
            Cell c3 = row.createCell(3);
            c3.setCellValue(formatOrderStatus(o.getStatus()));
            c3.setCellStyle(styles.dataCenter);

            // 4: Tổng tiền
            Cell c4 = row.createCell(4);
            c4.setCellValue(o.getTotalAmount() != null ? o.getTotalAmount().doubleValue() : 0.0);
            c4.setCellStyle(styles.currency);

            // 5: Giảm giá
            Cell c5 = row.createCell(5);
            c5.setCellValue(o.getDiscountAmount() != null ? o.getDiscountAmount().doubleValue() : 0.0);
            c5.setCellStyle(styles.currency);

            // 6: Phí vận chuyển
            Cell c6 = row.createCell(6);
            c6.setCellValue(o.getShippingFee() != null ? o.getShippingFee().doubleValue() : 0.0);
            c6.setCellStyle(styles.currency);

            // 7: Thành tiền cuối cùng
            Cell c7 = row.createCell(7);
            c7.setCellValue(o.getFinalAmount() != null ? o.getFinalAmount().doubleValue() : 0.0);
            c7.setCellStyle(styles.currency);

            // 8: Kênh bán hàng
            Cell c8 = row.createCell(8);
            c8.setCellValue(o.getSalesChannel() != null ? o.getSalesChannel().name() : "MOBILE");
            c8.setCellStyle(styles.dataCenter);
        }

        sheet.createFreezePane(0, 4);
        sheet.setAutoFilter(new CellRangeAddress(3, Math.max(3, r - 1), 0, headers.length - 1));
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1200, 3600));
        }
    }

    private void writeOrderDetailsSheet(Workbook workbook, Styles styles, List<OrderDetail> details) {
        Sheet sheet = workbook.createSheet("OrderDetails");
        sheet.setDisplayGridlines(true);

        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(28);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("CHI TIẾT MẶT HÀNG ĐƠN HÀNG");
        titleCell.setCellStyle(styles.title);

        Row subRow = sheet.createRow(1);
        subRow.setHeightInPoints(18);
        Cell subCell = subRow.createCell(0);
        subCell.setCellValue(
            "Thời gian xuất: " + LocalDateTime.now().format(DATE_FORMATTER) +
            " | Tổng số mặt hàng: " + details.size()
        );
        subCell.setCellStyle(styles.subtitle);

        String[] headers = {
            "Mã đơn hàng",
            "Sản phẩm",
            "SKU",
            "Kích thước",
            "Màu sắc",
            "Số lượng",
            "Đơn giá",
            "Thành tiền"
        };
        Row headerRow = sheet.createRow(3);
        headerRow.setHeightInPoints(26);
        for (int i = 0; i < headers.length; i++) {
            Cell c = headerRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(styles.header);
        }

        int r = 4;
        for (OrderDetail d : details) {
            Row row = sheet.createRow(r++);
            row.setHeightInPoints(20);

            // 0: Mã đơn hàng
            Cell c0 = row.createCell(0);
            c0.setCellValue(d.getId().getOrderId());
            c0.setCellStyle(styles.dataCenter);

            // 1: Sản phẩm
            Cell c1 = row.createCell(1);
            String pName = (d.getProductVariant() != null && d.getProductVariant().getProduct() != null)
                ? d.getProductVariant().getProduct().getProductName() : "";
            c1.setCellValue(pName);
            c1.setCellStyle(styles.dataLeft);

            // 2: SKU
            Cell c2 = row.createCell(2);
            c2.setCellValue(d.getProductVariant() != null ? d.getProductVariant().getSku() : "");
            c2.setCellStyle(styles.dataLeft);

            // 3: Kích thước
            Cell c3 = row.createCell(3);
            String size = (d.getProductVariant() != null && d.getProductVariant().getSize() != null)
                ? d.getProductVariant().getSize().getSizeName() : "";
            c3.setCellValue(size);
            c3.setCellStyle(styles.dataCenter);

            // 4: Màu sắc
            Cell c4 = row.createCell(4);
            String color = (d.getProductVariant() != null && d.getProductVariant().getColor() != null)
                ? d.getProductVariant().getColor().getColorName() : "";
            c4.setCellValue(color);
            c4.setCellStyle(styles.dataCenter);

            // 5: Số lượng
            Cell c5 = row.createCell(5);
            c5.setCellValue(d.getQuantity() != null ? d.getQuantity() : 0);
            c5.setCellStyle(styles.dataNumber);

            // 6: Đơn giá
            Cell c6 = row.createCell(6);
            c6.setCellValue(d.getUnitPrice() != null ? d.getUnitPrice().doubleValue() : 0.0);
            c6.setCellStyle(styles.currency);

            // 7: Thành tiền
            Cell c7 = row.createCell(7);
            c7.setCellValue(d.getSubtotal() != null ? d.getSubtotal().doubleValue() : 0.0);
            c7.setCellStyle(styles.currency);
        }

        sheet.createFreezePane(0, 4);
        sheet.setAutoFilter(new CellRangeAddress(3, Math.max(3, r - 1), 0, headers.length - 1));
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1200, 3600));
        }
    }

    private String buildFilterInfo(String keyword, OrderStatus status, LocalDateTime from, LocalDateTime to, Long customerId) {
        List<String> list = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) list.add("Từ khóa: \"" + keyword.strip() + "\"");
        if (status != null) list.add("Trạng thái: " + formatOrderStatus(status));
        if (from != null) list.add("Từ: " + from.format(DATE_FORMATTER));
        if (to != null) list.add("Đến: " + to.format(DATE_FORMATTER));
        if (customerId != null) list.add("Khách hàng ID: #" + customerId);
        return list.isEmpty() ? "Tất cả" : String.join(", ", list);
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

        CellStyle dataLeft = workbook.createCellStyle();
        dataLeft.setFont(dataFont);
        dataLeft.setAlignment(HorizontalAlignment.LEFT);
        dataLeft.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(dataLeft);

        CellStyle dataCenter = workbook.createCellStyle();
        dataCenter.cloneStyleFrom(dataLeft);
        dataCenter.setAlignment(HorizontalAlignment.CENTER);

        CellStyle currency = workbook.createCellStyle();
        currency.cloneStyleFrom(dataLeft);
        currency.setAlignment(HorizontalAlignment.RIGHT);
        currency.setDataFormat(dataFormat.getFormat("#,##0 \"₫\""));

        CellStyle dataNumber = workbook.createCellStyle();
        dataNumber.cloneStyleFrom(dataLeft);
        dataNumber.setAlignment(HorizontalAlignment.RIGHT);
        dataNumber.setDataFormat(dataFormat.getFormat("#,##0"));

        CellStyle dataDate = workbook.createCellStyle();
        dataDate.cloneStyleFrom(dataLeft);
        dataDate.setAlignment(HorizontalAlignment.CENTER);
        dataDate.setDataFormat(dataFormat.getFormat("dd/mm/yyyy hh:mm:ss"));

        return new Styles(title, subtitle, header, dataLeft, dataCenter, currency, dataNumber, dataDate);
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
        CellStyle dataCenter,
        CellStyle currency,
        CellStyle dataNumber,
        CellStyle dataDate
    ) {}
}

