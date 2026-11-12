package com.lauriel.store.controller.web;

import com.lauriel.store.service.CustomerExcelExportService;
import com.lauriel.store.service.CustomerService;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class WebCustomerController {

    private final CustomerService customers;
    private final CustomerExcelExportService exportService;

    public WebCustomerController(
        CustomerService customers,
        CustomerExcelExportService exportService
    ) {
        this.customers = customers;
        this.exportService = exportService;
    }

    @GetMapping("/admin/customers/export.xlsx")
    public void exportExcel(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) com.lauriel.store.entity.enums.UserStatus status,
        HttpServletResponse response
    ) throws IOException {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .format(LocalDateTime.now());
        String filename = "customers_" + timestamp + ".xlsx";

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + filename + "\""
        );
        exportService.exportCustomers(keyword, status, response.getOutputStream());
        response.flushBuffer();
    }

    @GetMapping("/admin/customers")
    public String list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size,
        @RequestParam(required = false) String keyword,
        @RequestParam(
            required = false
        ) com.lauriel.store.entity.enums.UserStatus status,
        Model m
    ) {
        m.addAttribute("page", customers.list(page, size, keyword, status));
        m.addAttribute("keyword", keyword);
        m.addAttribute("status", status);
        return "admin/customers/list";
    }
}
