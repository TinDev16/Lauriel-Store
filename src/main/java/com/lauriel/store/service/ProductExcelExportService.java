package com.lauriel.store.service;

import com.lauriel.store.entity.Product;
import com.lauriel.store.entity.ProductVariant;
import com.lauriel.store.entity.enums.ActiveStatus;
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
public class ProductExcelExportService {

    private static final DateTimeFormatter DISPLAY_DATE_FORMAT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final ProductService productService;
    private final ProductVariantService productVariantService;

    public ProductExcelExportService(
        ProductService productService,
        ProductVariantService productVariantService
    ) {
        this.productService = productService;
        this.productVariantService = productVariantService;
    }

    @Transactional(readOnly = true)
    public void exportProducts(
        String keyword,
        Long categoryId,
        ActiveStatus status,
        OutputStream outputStream
    ) throws IOException {
        List<Product> products = productService.listForExport(keyword, categoryId, status);
        List<Long> productIds = products.stream().map(Product::getProductId).toList();
        List<ProductVariant> variants = productVariantService.listForExport(productIds);

        try (Workbook workbook = new XSSFWorkbook()) {
            Styles styles = createStyles(workbook);

            writeProductSheet(workbook, styles, products, keyword, categoryId, status);
            writeVariantSheet(workbook, styles, variants, keyword, categoryId, status);

            workbook.write(outputStream);
        }
    }

    private void writeProductSheet(
        Workbook workbook,
        Styles styles,
        List<Product> products,
        String keyword,
        Long categoryId,
        ActiveStatus status
    ) {
        Sheet sheet = workbook.createSheet("Products");
        sheet.setDisplayGridlines(true);

        // Row 0: Title
        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(28);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("DANH SÁCH SẢN PHẨM");
        titleCell.setCellStyle(styles.title);

        // Row 1: Subtitle & Filter info
        Row subtitleRow = sheet.createRow(1);
        subtitleRow.setHeightInPoints(18);
        Cell subtitleCell = subtitleRow.createCell(0);
        String filterInfo = buildFilterText(keyword, categoryId, status);
        subtitleCell.setCellValue(
            "Thời gian xuất: " + LocalDateTime.now().format(DISPLAY_DATE_FORMAT) +
            " | Tổng số: " + products.size() + " sản phẩm" +
            " | Điều kiện lọc: " + filterInfo
        );
        subtitleCell.setCellStyle(styles.subtitle);

        // Row 2: Blank spacing row
        sheet.createRow(2).setHeightInPoints(10);

        // Row 3: Header
        String[] headers = {
            "Mã sản phẩm",
            "Tên sản phẩm",
            "Danh mục",
            "Mô tả",
            "Giá sản phẩm",
            "Trạng thái",
            "Ngày tạo"
        };

        Row headerRow = sheet.createRow(3);
        headerRow.setHeightInPoints(26);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.header);
        }

        // Data Rows starting at index 4
        int rowIndex = 4;
        for (Product product : products) {
            Row row = sheet.createRow(rowIndex++);
            row.setHeightInPoints(20);

            // 0: Mã sản phẩm
            Cell cell0 = row.createCell(0);
            if (product.getProductId() != null) {
                cell0.setCellValue(product.getProductId());
            }
            cell0.setCellStyle(styles.dataCenter);

            // 1: Tên sản phẩm
            Cell cell1 = row.createCell(1);
            cell1.setCellValue(product.getProductName() != null ? product.getProductName() : "");
            cell1.setCellStyle(styles.dataLeft);

            // 2: Danh mục
            Cell cell2 = row.createCell(2);
            String categoryName = product.getCategory() != null ? product.getCategory().getCategoryName() : "";
            cell2.setCellValue(categoryName);
            cell2.setCellStyle(styles.dataLeft);

            // 3: Mô tả
            Cell cell3 = row.createCell(3);
            cell3.setCellValue(product.getDescription() != null ? product.getDescription() : "");
            cell3.setCellStyle(styles.dataWrap);

            // 4: Giá sản phẩm
            Cell cell4 = row.createCell(4);
            if (product.getPrice() != null) {
                cell4.setCellValue(product.getPrice().doubleValue());
            } else {
                cell4.setCellValue(0.0);
            }
            cell4.setCellStyle(styles.dataCurrency);

            // 5: Trạng thái
            Cell cell5 = row.createCell(5);
            String statusText = product.getStatus() == ActiveStatus.ACTIVE ? "Đang hoạt động" : "Ngừng hoạt động";
            cell5.setCellValue(statusText);
            cell5.setCellStyle(styles.dataCenter);

            // 6: Ngày tạo
            Cell cell6 = row.createCell(6);
            if (product.getCreatedAt() != null) {
                cell6.setCellValue(product.getCreatedAt());
                cell6.setCellStyle(styles.dataDate);
            } else {
                cell6.setCellValue("—");
                cell6.setCellStyle(styles.dataCenter);
            }
        }

        // Freeze Pane at row 4 (freezes rows 0-3)
        sheet.createFreezePane(0, 4);

        // Auto filter
        int lastDataRow = Math.max(3, rowIndex - 1);
        sheet.setAutoFilter(new CellRangeAddress(3, lastDataRow, 0, headers.length - 1));

        // Auto-size columns with padding
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            int paddedWidth = Math.max(currentWidth + 1200, 3600);
            if (i == 3) {
                // Limit description column width
                paddedWidth = Math.min(paddedWidth, 16000);
            }
            sheet.setColumnWidth(i, paddedWidth);
        }
    }

    private void writeVariantSheet(
        Workbook workbook,
        Styles styles,
        List<ProductVariant> variants,
        String keyword,
        Long categoryId,
        ActiveStatus status
    ) {
        Sheet sheet = workbook.createSheet("Variants");
        sheet.setDisplayGridlines(true);

        // Row 0: Title
        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(28);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("DANH SÁCH BIẾN THỂ SẢN PHẨM");
        titleCell.setCellStyle(styles.title);

        // Row 1: Subtitle & Note
        Row subtitleRow = sheet.createRow(1);
        subtitleRow.setHeightInPoints(18);
        Cell subtitleCell = subtitleRow.createCell(0);
        subtitleCell.setCellValue(
            "Thời gian xuất: " + LocalDateTime.now().format(DISPLAY_DATE_FORMAT) +
            " | Tổng số: " + variants.size() + " biến thể" +
            " | Lưu ý: Số lượng tồn kho chỉ phục vụ xem và đối chiếu, không được sử dụng để nhập đè số lượng trong database."
        );
        subtitleCell.setCellStyle(styles.subtitle);

        // Row 2: Blank spacing row
        sheet.createRow(2).setHeightInPoints(10);

        // Row 3: Header
        String[] headers = {
            "Mã biến thể",
            "Mã sản phẩm",
            "Tên sản phẩm",
            "SKU",
            "Kích thước",
            "Màu sắc",
            "Giá biến thể",
            "Trạng thái",
            "Số lượng tồn kho"
        };

        Row headerRow = sheet.createRow(3);
        headerRow.setHeightInPoints(26);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.header);
        }

        // Data Rows starting at index 4
        int rowIndex = 4;
        for (ProductVariant variant : variants) {
            Row row = sheet.createRow(rowIndex++);
            row.setHeightInPoints(20);

            // 0: Mã biến thể
            Cell cell0 = row.createCell(0);
            if (variant.getVariantId() != null) {
                cell0.setCellValue(variant.getVariantId());
            }
            cell0.setCellStyle(styles.dataCenter);

            // 1: Mã sản phẩm
            Cell cell1 = row.createCell(1);
            if (variant.getProduct() != null && variant.getProduct().getProductId() != null) {
                cell1.setCellValue(variant.getProduct().getProductId());
            }
            cell1.setCellStyle(styles.dataCenter);

            // 2: Tên sản phẩm
            Cell cell2 = row.createCell(2);
            String productName = variant.getProduct() != null ? variant.getProduct().getProductName() : "";
            cell2.setCellValue(productName);
            cell2.setCellStyle(styles.dataLeft);

            // 3: SKU
            Cell cell3 = row.createCell(3);
            cell3.setCellValue(variant.getSku() != null ? variant.getSku() : "");
            cell3.setCellStyle(styles.dataLeft);

            // 4: Kích thước
            Cell cell4 = row.createCell(4);
            String sizeName = variant.getSize() != null ? variant.getSize().getSizeName() : "";
            cell4.setCellValue(sizeName);
            cell4.setCellStyle(styles.dataCenter);

            // 5: Màu sắc
            Cell cell5 = row.createCell(5);
            String colorName = variant.getColor() != null ? variant.getColor().getColorName() : "";
            cell5.setCellValue(colorName);
            cell5.setCellStyle(styles.dataCenter);

            // 6: Giá biến thể
            Cell cell6 = row.createCell(6);
            if (variant.getPrice() != null) {
                cell6.setCellValue(variant.getPrice().doubleValue());
            } else {
                cell6.setCellValue(0.0);
            }
            cell6.setCellStyle(styles.dataCurrency);

            // 7: Trạng thái
            Cell cell7 = row.createCell(7);
            String statusText = variant.getStatus() == ActiveStatus.ACTIVE ? "Đang hoạt động" : "Ngừng hoạt động";
            cell7.setCellValue(statusText);
            cell7.setCellStyle(styles.dataCenter);

            // 8: Số lượng tồn kho
            Cell cell8 = row.createCell(8);
            cell8.setCellValue(variant.getQuantity() != null ? variant.getQuantity() : 0);
            cell8.setCellStyle(styles.dataNumber);
        }

        // Freeze Pane at row 4
        sheet.createFreezePane(0, 4);

        // Auto filter
        int lastDataRow = Math.max(3, rowIndex - 1);
        sheet.setAutoFilter(new CellRangeAddress(3, lastDataRow, 0, headers.length - 1));

        // Auto-size columns with padding
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.max(currentWidth + 1200, 3600));
        }
    }

    private String buildFilterText(String keyword, Long categoryId, ActiveStatus status) {
        List<String> filters = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            filters.add("Từ khóa: \"" + keyword.strip() + "\"");
        }
        if (categoryId != null) {
            filters.add("Danh mục ID: #" + categoryId);
        }
        if (status != null) {
            filters.add("Trạng thái: " + (status == ActiveStatus.ACTIVE ? "Đang hoạt động" : "Ngừng hoạt động"));
        }
        return filters.isEmpty() ? "Tất cả" : String.join(", ", filters);
    }

    private Styles createStyles(Workbook workbook) {
        DataFormat dataFormat = workbook.createDataFormat();

        // Title Style
        Font titleFont = workbook.createFont();
        titleFont.setFontName("Calibri");
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setBold(true);
        titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());

        CellStyle title = workbook.createCellStyle();
        title.setFont(titleFont);
        title.setAlignment(HorizontalAlignment.LEFT);
        title.setVerticalAlignment(VerticalAlignment.CENTER);

        // Subtitle Style
        Font subtitleFont = workbook.createFont();
        subtitleFont.setFontName("Calibri");
        subtitleFont.setFontHeightInPoints((short) 10);
        subtitleFont.setItalic(true);
        subtitleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

        CellStyle subtitle = workbook.createCellStyle();
        subtitle.setFont(subtitleFont);
        subtitle.setAlignment(HorizontalAlignment.LEFT);
        subtitle.setVerticalAlignment(VerticalAlignment.CENTER);

        // Header Style
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

        // Data Font
        Font dataFont = workbook.createFont();
        dataFont.setFontName("Calibri");
        dataFont.setFontHeightInPoints((short) 10);

        // Data Left
        CellStyle dataLeft = workbook.createCellStyle();
        dataLeft.setFont(dataFont);
        dataLeft.setAlignment(HorizontalAlignment.LEFT);
        dataLeft.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(dataLeft);

        // Data Center
        CellStyle dataCenter = workbook.createCellStyle();
        dataCenter.cloneStyleFrom(dataLeft);
        dataCenter.setAlignment(HorizontalAlignment.CENTER);

        // Data Currency
        CellStyle dataCurrency = workbook.createCellStyle();
        dataCurrency.cloneStyleFrom(dataLeft);
        dataCurrency.setAlignment(HorizontalAlignment.RIGHT);
        dataCurrency.setDataFormat(dataFormat.getFormat("#,##0 \"₫\""));

        // Data Number (Integer)
        CellStyle dataNumber = workbook.createCellStyle();
        dataNumber.cloneStyleFrom(dataLeft);
        dataNumber.setAlignment(HorizontalAlignment.RIGHT);
        dataNumber.setDataFormat(dataFormat.getFormat("#,##0"));

        // Data Date
        CellStyle dataDate = workbook.createCellStyle();
        dataDate.cloneStyleFrom(dataLeft);
        dataDate.setAlignment(HorizontalAlignment.CENTER);
        dataDate.setDataFormat(dataFormat.getFormat("dd/mm/yyyy hh:mm:ss"));

        // Data Wrap
        CellStyle dataWrap = workbook.createCellStyle();
        dataWrap.cloneStyleFrom(dataLeft);
        dataWrap.setWrapText(true);

        return new Styles(title, subtitle, header, dataLeft, dataCenter, dataCurrency, dataNumber, dataDate, dataWrap);
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
        CellStyle dataCurrency,
        CellStyle dataNumber,
        CellStyle dataDate,
        CellStyle dataWrap
    ) {}
}

