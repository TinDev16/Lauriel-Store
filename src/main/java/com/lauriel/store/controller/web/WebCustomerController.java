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
        com.lauriel.store.entity.enums.UserStatus effectiveStatus =
            (status != null) ? status : com.lauriel.store.entity.enums.UserStatus.ACTIVE;
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .format(LocalDateTime.now());
        String filename = "customers_" + timestamp + ".xlsx";

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + filename + "\""
        );
        exportService.exportCustomers(keyword, effectiveStatus, response.getOutputStream());
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
        com.lauriel.store.entity.enums.UserStatus effectiveStatus =
            (status != null) ? status : com.lauriel.store.entity.enums.UserStatus.ACTIVE;
        m.addAttribute("page", customers.list(page, size, keyword, effectiveStatus));
        m.addAttribute("keyword", keyword);
        m.addAttribute("status", status);
        m.addAttribute("effectiveStatus", effectiveStatus);
        m.addAttribute("activeCount", customers.countByStatus(com.lauriel.store.entity.enums.UserStatus.ACTIVE));
        m.addAttribute("inactiveCount", customers.countByStatus(com.lauriel.store.entity.enums.UserStatus.INACTIVE));
        return "admin/customers/list";
    }

    @GetMapping("/admin/customers/inactive")
    public String listInactive(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size,
        @RequestParam(required = false) String keyword,
        Model m
    ) {
        return list(page, size, keyword, com.lauriel.store.entity.enums.UserStatus.INACTIVE, m);
    }
}
