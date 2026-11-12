package com.lauriel.store.service;

import com.lauriel.store.entity.InventoryTransaction;
import com.lauriel.store.entity.ProductVariant;
import com.lauriel.store.entity.enums.ActiveStatus;
import com.lauriel.store.entity.enums.InventoryTransactionType;
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
public class InventoryExcelExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final InventoryService inventoryService;

    public InventoryExcelExportService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Transactional(readOnly = true)
    public void exportInventory(
        String keyword,
        Integer threshold,
        Long variantId,
        InventoryTransactionType type,
        LocalDateTime from,
        LocalDateTime to,
        OutputStream outputStream
    ) throws IOException {
        List<ProductVariant> variants = inventoryService.listVariantsForExport(keyword, threshold, variantId);
        List<InventoryTransaction> transactions = inventoryService.listTransactionsForExport(
            keyword,
            variantId,
            type,
            from,
            to
        );

        try (Workbook workbook = new XSSFWorkbook()) {
            Styles styles = createStyles(workbook);

            writeInventorySheet(workbook, styles, variants, keyword, threshold, variantId);
            writeTransactionsSheet(workbook, styles, transactions, keyword, variantId, type, from, to);

            workbook.write(outputStream);
        }
    }

    private void writeInventorySheet(
        Workbook workbook,
        Styles styles,
        List<ProductVariant> variants,
        String keyword,
        Integer threshold,
        Long variantId
    ) {
        Sheet sheet = workbook.createSheet("Inventory");
        sheet.setDisplayGridlines(true);

        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(28);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("DANH SÁCH TỒN KHO THEO BIẾN THỂ");
        titleCell.setCellStyle(styles.title);

        Row subRow = sheet.createRow(1);
        subRow.setHeightInPoints(18);
        Cell subCell = subRow.createCell(0);
        subCell.setCellValue(
            "Thời gian xuất: " + LocalDateTime.now().format(DATE_FORMATTER) +
            " | Tổng số: " + variants.size() + " biến thể" +
            " | Điều kiện lọc: " + buildInventoryFilterInfo(keyword, threshold, variantId)
        );
        subCell.setCellStyle(styles.subtitle);

        String[] headers = {
            "Mã biến thể",
            "Mã sản phẩm",
            "Tên sản phẩm",
            "SKU",
            "Kích thước",
            "Màu sắc",
            "Giá bán",
            "Số lượng tồn",
            "Trạng thái"
        };
        Row headerRow = sheet.createRow(3);
        headerRow.setHeightInPoints(26);
        for (int i = 0; i < headers.length; i++) {
            Cell c = headerRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(styles.header);
        }

        int r = 4;
        for (ProductVariant v : variants) {
            Row row = sheet.createRow(r++);
            row.setHeightInPoints(20);

            // 0: Mã biến thể
            Cell c0 = row.createCell(0);
            c0.setCellValue(v.getVariantId());
            c0.setCellStyle(styles.dataCenter);

            // 1: Mã sản phẩm
            Cell c1 = row.createCell(1);
            c1.setCellValue(v.getProduct() != null ? v.getProduct().getProductId() : 0);
            c1.setCellStyle(styles.dataCenter);

            // 2: Tên sản phẩm
            Cell c2 = row.createCell(2);
            c2.setCellValue(v.getProduct() != null ? v.getProduct().getProductName() : "");
            c2.setCellStyle(styles.dataLeft);

            // 3: SKU
            Cell c3 = row.createCell(3);
            c3.setCellValue(v.getSku() != null ? v.getSku() : "");
            c3.setCellStyle(styles.dataLeft);

            // 4: Kích thước
            Cell c4 = row.createCell(4);
            c4.setCellValue(v.getSize() != null ? v.getSize().getSizeName() : "—");
            c4.setCellStyle(styles.dataCenter);

            // 5: Màu sắc
            Cell c5 = row.createCell(5);
            c5.setCellValue(v.getColor() != null ? v.getColor().getColorName() : "—");
            c5.setCellStyle(styles.dataCenter);

            // 6: Giá bán
            Cell c6 = row.createCell(6);
            if (v.getPrice() != null) {
                c6.setCellValue(v.getPrice().doubleValue());
                c6.setCellStyle(styles.currency);
            } else {
                c6.setCellValue(0);
                c6.setCellStyle(styles.currency);
            }

            // 7: Số lượng tồn
            Cell c7 = row.createCell(7);
            c7.setCellValue(v.getQuantity() != null ? v.getQuantity() : 0);
            c7.setCellStyle(styles.dataNumber);

            // 8: Trạng thái
            Cell c8 = row.createCell(8);
            c8.setCellValue(formatStatus(v.getStatus()));
            c8.setCellStyle(styles.dataCenter);
        }

        sheet.createFreezePane(0, 4);
        int lastRow = Math.max(4, r - 1);
        sheet.setAutoFilter(new CellRangeAddress(3, lastRow, 0, headers.length - 1));

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i) + 1000;
            sheet.setColumnWidth(i, Math.min(Math.max(width, 3000), 12000));
        }
    }

    private void writeTransactionsSheet(
        Workbook workbook,
        Styles styles,
        List<InventoryTransaction> transactions,
        String keyword,
        Long variantId,
        InventoryTransactionType type,
        LocalDateTime from,
        LocalDateTime to
    ) {
        Sheet sheet = workbook.createSheet("TransactionHistory");
        sheet.setDisplayGridlines(true);

        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(28);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("LỊCH SỬ GIAO DỊCH XUẤT NHẬP KHO");
        titleCell.setCellStyle(styles.title);

        Row subRow = sheet.createRow(1);
        subRow.setHeightInPoints(18);
        Cell subCell = subRow.createCell(0);
        subCell.setCellValue(
            "Thời gian xuất: " + LocalDateTime.now().format(DATE_FORMATTER) +
            " | Tổng số: " + transactions.size() + " giao dịch" +
            " | Điều kiện lọc: " + buildTransactionFilterInfo(keyword, variantId, type, from, to)
        );
        subCell.setCellStyle(styles.subtitle);

        String[] headers = {
            "Mã giao dịch",
            "Thời gian",
            "Sản phẩm",
            "SKU",
            "Loại giao dịch",
            "Số lượng",
            "Người thực hiện",
            "Loại chứng từ tham chiếu",
            "Mã chứng từ tham chiếu",
            "Ghi chú"
        };
        Row headerRow = sheet.createRow(3);
        headerRow.setHeightInPoints(26);
        for (int i = 0; i < headers.length; i++) {
            Cell c = headerRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(styles.header);
        }

        int r = 4;
        for (InventoryTransaction tx : transactions) {
            Row row = sheet.createRow(r++);
            row.setHeightInPoints(20);

            // 0: Mã giao dịch
            Cell c0 = row.createCell(0);
            c0.setCellValue(tx.getTransactionId());
            c0.setCellStyle(styles.dataCenter);

            // 1: Thời gian
            Cell c1 = row.createCell(1);
            if (tx.getCreatedAt() != null) {
                c1.setCellValue(tx.getCreatedAt().format(DATE_FORMATTER));
            } else {
                c1.setCellValue("—");
            }
            c1.setCellStyle(styles.dataCenter);

            // 2: Sản phẩm
            Cell c2 = row.createCell(2);
            String prodName = (tx.getProductVariant() != null && tx.getProductVariant().getProduct() != null)
                ? tx.getProductVariant().getProduct().getProductName()
                : "—";
            c2.setCellValue(prodName);
            c2.setCellStyle(styles.dataLeft);

            // 3: SKU
            Cell c3 = row.createCell(3);
            String sku = tx.getProductVariant() != null ? tx.getProductVariant().getSku() : "—";
            c3.setCellValue(sku != null ? sku : "—");
            c3.setCellStyle(styles.dataLeft);

            // 4: Loại giao dịch
            Cell c4 = row.createCell(4);
            c4.setCellValue(formatTransactionType(tx.getTransactionType()));
            c4.setCellStyle(styles.dataCenter);

            // 5: Số lượng
            Cell c5 = row.createCell(5);
            c5.setCellValue(tx.getQuantity() != null ? tx.getQuantity() : 0);
            c5.setCellStyle(styles.dataNumber);

            // 6: Người thực hiện
            Cell c6 = row.createCell(6);
            String userStr = "—";
            if (tx.getUser() != null) {
                if (tx.getUser().getFullName() != null && !tx.getUser().getFullName().isBlank()) {
                    userStr = tx.getUser().getFullName();
                } else if (tx.getUser().getUsername() != null) {
                    userStr = tx.getUser().getUsername();
                }
            }
            c6.setCellValue(userStr);
            c6.setCellStyle(styles.dataLeft);

            // 7: Loại chứng từ tham chiếu
            Cell c7 = row.createCell(7);
            c7.setCellValue(tx.getReferenceType() != null ? tx.getReferenceType() : "—");
            c7.setCellStyle(styles.dataCenter);

            // 8: Mã chứng từ tham chiếu
            Cell c8 = row.createCell(8);
            if (tx.getReferenceId() != null) {
                c8.setCellValue(tx.getReferenceId());
                c8.setCellStyle(styles.dataCenter);
            } else {
                c8.setCellValue("—");
                c8.setCellStyle(styles.dataCenter);
            }

            // 9: Ghi chú
            Cell c9 = row.createCell(9);
            c9.setCellValue(tx.getNote() != null ? tx.getNote() : "");
            c9.setCellStyle(styles.dataLeft);
        }

        sheet.createFreezePane(0, 4);
        int lastRow = Math.max(4, r - 1);
        sheet.setAutoFilter(new CellRangeAddress(3, lastRow, 0, headers.length - 1));

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i) + 1000;
            sheet.setColumnWidth(i, Math.min(Math.max(width, 3000), 12000));
        }
    }

    private String buildInventoryFilterInfo(String keyword, Integer threshold, Long variantId) {
        List<String> list = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) list.add("Từ khóa: \"" + keyword.strip() + "\"");
        if (threshold != null) list.add("Ngưỡng tồn <= " + threshold);
        if (variantId != null) list.add("Biến thể ID: #" + variantId);
        return list.isEmpty() ? "Tất cả" : String.join(", ", list);
    }

    private String buildTransactionFilterInfo(
        String keyword,
        Long variantId,
        InventoryTransactionType type,
        LocalDateTime from,
        LocalDateTime to
    ) {
        List<String> list = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) list.add("Từ khóa: \"" + keyword.strip() + "\"");
        if (variantId != null) list.add("Biến thể ID: #" + variantId);
        if (type != null) list.add("Loại giao dịch: " + formatTransactionType(type));
        if (from != null) list.add("Từ: " + from.format(DATE_FORMATTER));
        if (to != null) list.add("Đến: " + to.format(DATE_FORMATTER));
        return list.isEmpty() ? "Tất cả" : String.join(", ", list);
    }

    private String formatStatus(ActiveStatus status) {
        if (status == null) return "—";
        return status == ActiveStatus.ACTIVE ? "Đang bán" : "Ngừng bán";
    }

    private String formatTransactionType(InventoryTransactionType type) {
        if (type == null) return "—";
        return switch (type) {
            case IMPORT -> "Nhập hàng";
            case SALE -> "Bán hàng";
            case RETURN -> "Hoàn trả";
            case ADJUSTMENT -> "Điều chỉnh";
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

        return new Styles(title, subtitle, header, dataLeft, dataCenter, currency, dataNumber);
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
        CellStyle dataNumber
    ) {}
}

