package com.lauriel.store.service;

import com.lauriel.store.entity.Customer;
import com.lauriel.store.entity.enums.UserStatus;
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
public class CustomerExcelExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final CustomerService customerService;

    public CustomerExcelExportService(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Transactional(readOnly = true)
    public void exportCustomers(
        String keyword,
        UserStatus status,
        OutputStream outputStream
    ) throws IOException {
        List<Customer> customerList = customerService.listForExport(keyword, status);

        try (Workbook workbook = new XSSFWorkbook()) {
            Styles styles = createStyles(workbook);

            Sheet sheet = workbook.createSheet("Customers");
            sheet.setDisplayGridlines(true);

            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(28);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("DANH SÁCH KHÁCH HÀNG");
            titleCell.setCellStyle(styles.title);

            Row subRow = sheet.createRow(1);
            subRow.setHeightInPoints(18);
            Cell subCell = subRow.createCell(0);
            subCell.setCellValue(
                "Thời gian xuất: " + LocalDateTime.now().format(DATE_FORMATTER) +
                " | Tổng số: " + customerList.size() + " khách hàng" +
                " | Điều kiện lọc: " + buildFilterInfo(keyword, status)
            );
            subCell.setCellStyle(styles.subtitle);

            String[] headers = {
                "Mã khách hàng",
                "Họ tên",
                "Số điện thoại",
                "Email",
                "Ngày tạo"
            };
            Row headerRow = sheet.createRow(3);
            headerRow.setHeightInPoints(26);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(styles.header);
            }

            int r = 4;
            for (Customer c : customerList) {
                Row row = sheet.createRow(r++);
                row.setHeightInPoints(20);

                // 0: Mã khách hàng
                Cell c0 = row.createCell(0);
                c0.setCellValue(c.getCustomerId());
                c0.setCellStyle(styles.dataCenter);

                // 1: Họ tên
                Cell c1 = row.createCell(1);
                c1.setCellValue(c.getCustomerName() != null ? c.getCustomerName() : "");
                c1.setCellStyle(styles.dataLeft);

                // 2: Số điện thoại
                Cell c2 = row.createCell(2);
                c2.setCellValue(c.getPhone() != null ? c.getPhone() : "—");
                c2.setCellStyle(styles.dataCenter);

                // 3: Email
                Cell c3 = row.createCell(3);
                String email = (c.getUser() != null && c.getUser().getEmail() != null)
                    ? c.getUser().getEmail()
                    : "—";
                c3.setCellValue(email);
                c3.setCellStyle(styles.dataLeft);

                // 4: Ngày tạo
                Cell c4 = row.createCell(4);
                if (c.getCreatedAt() != null) {
                    c4.setCellValue(c.getCreatedAt().format(DATE_FORMATTER));
                } else {
                    c4.setCellValue("—");
                }
                c4.setCellStyle(styles.dataCenter);
            }

            sheet.createFreezePane(0, 4);
            int lastRow = Math.max(4, r - 1);
            sheet.setAutoFilter(new CellRangeAddress(3, lastRow, 0, headers.length - 1));

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int width = sheet.getColumnWidth(i) + 1200;
                sheet.setColumnWidth(i, Math.min(Math.max(width, 3500), 12000));
            }

            workbook.write(outputStream);
        }
    }

    private String buildFilterInfo(String keyword, UserStatus status) {
        List<String> list = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) list.add("Từ khóa: \"" + keyword.strip() + "\"");
        if (status != null) list.add("Trạng thái: " + (status == UserStatus.ACTIVE ? "Hoạt động" : "Bị khóa"));
        return list.isEmpty() ? "Tất cả" : String.join(", ", list);
    }

    private Styles createStyles(Workbook workbook) {
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

        return new Styles(title, subtitle, header, dataLeft, dataCenter);
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
        CellStyle dataCenter
    ) {}
}

