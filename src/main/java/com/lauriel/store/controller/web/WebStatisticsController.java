package com.lauriel.store.controller.web;

import com.lauriel.store.dto.statistics.StatisticsReport;
import com.lauriel.store.service.StatisticsExcelExportService;
import com.lauriel.store.service.StatisticsService;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WebStatisticsController {

    private final StatisticsService statistics;
    private final StatisticsExcelExportService exportService;
    private final com.lauriel.store.service.StatisticsPdfExportService pdfExportService;
    private final WebViewSupport viewSupport;

    public WebStatisticsController(
        StatisticsService statistics,
        StatisticsExcelExportService exportService,
        com.lauriel.store.service.StatisticsPdfExportService pdfExportService,
        WebViewSupport viewSupport
    ) {
        this.statistics = statistics;
        this.exportService = exportService;
        this.pdfExportService = pdfExportService;
        this.viewSupport = viewSupport;
    }

    @GetMapping("/admin/statistics/print")
    public String printReport(
        @RequestParam(defaultValue = "overview") String section,
        @RequestParam(required = false) String preset,
        @RequestParam(required = false) @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        ) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        ) LocalDate toDate,
        @RequestParam(required = false) String groupBy,
        @RequestParam(defaultValue = "5") int threshold,
        @RequestParam(defaultValue = "5") int top,
        Model model
    ) {
        addReport(
            model,
            section,
            preset,
            fromDate,
            toDate,
            groupBy,
            threshold,
            top
        );
        model.addAttribute("preset", preset);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("groupBy", groupBy);
        model.addAttribute("threshold", threshold);
        model.addAttribute("top", top);
        return "admin/statistics-print";
    }

    @GetMapping("/admin/statistics/export.pdf")
    public void exportPdf(
        @RequestParam(defaultValue = "overview") String section,
        @RequestParam(required = false) String preset,
        @RequestParam(required = false) @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        ) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        ) LocalDate toDate,
        @RequestParam(required = false) String groupBy,
        @RequestParam(defaultValue = "5") int threshold,
        @RequestParam(defaultValue = "5") int top,
        HttpServletResponse response
    ) throws IOException {
        StatisticsReport report = statistics.report(
            preset,
            fromDate,
            toDate,
            groupBy,
            threshold,
            top
        );

        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .format(LocalDateTime.now());
        String filename = "statistics_report_" + (section != null ? section + "_" : "") + timestamp + ".pdf";

        response.setContentType("application/pdf");
        response.setHeader(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + filename + "\""
        );
        pdfExportService.exportReportPdf(
            report,
            section,
            preset,
            fromDate,
            toDate,
            groupBy,
            threshold,
            top,
            response.getOutputStream()
        );
        response.flushBuffer();
    }

    @GetMapping("/admin/statistics/export.xlsx")
    public void exportExcel(
        @RequestParam(required = false) String section,
        @RequestParam(required = false) String preset,
        @RequestParam(required = false) @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        ) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        ) LocalDate toDate,
        @RequestParam(required = false) String groupBy,
        @RequestParam(defaultValue = "5") int threshold,
        @RequestParam(defaultValue = "5") int top,
        HttpServletResponse response
    ) throws IOException {
        StatisticsReport report = statistics.report(
            preset,
            fromDate,
            toDate,
            groupBy,
            threshold,
            top
        );

        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .format(LocalDateTime.now());
        String filename = "statistics_report_" + timestamp + ".xlsx";

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + filename + "\""
        );
        exportService.exportReport(report, section, response.getOutputStream());
        response.flushBuffer();
    }

    @GetMapping("/admin/statistics")
    public String dashboard(
        @RequestParam(required = false) String preset,
        @RequestParam(required = false) @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        ) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        ) LocalDate toDate,
        @RequestParam(required = false) String groupBy,
        @RequestParam(defaultValue = "5") int threshold,
        @RequestParam(defaultValue = "5") int top,
        Model model
    ) {
        addReport(
            model,
            "overview",
            preset,
            fromDate,
            toDate,
            groupBy,
            threshold,
            top
        );
        return "admin/statistics";
    }

    @GetMapping("/admin/statistics/sales")
    public String sales(
        @RequestParam(required = false) String preset,
        @RequestParam(required = false) @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        ) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        ) LocalDate toDate,
        @RequestParam(required = false) String groupBy,
        @RequestParam(defaultValue = "5") int threshold,
        @RequestParam(defaultValue = "5") int top,
        Model model
    ) {
        return detail(
            model,
            "sales",
            preset,
            fromDate,
            toDate,
            groupBy,
            threshold,
            top
        );
    }

    @GetMapping("/admin/statistics/products")
    public String products(
        @RequestParam(required = false) String preset,
        @RequestParam(required = false) @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        ) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        ) LocalDate toDate,
        @RequestParam(required = false) String groupBy,
        @RequestParam(defaultValue = "5") int threshold,
        @RequestParam(defaultValue = "5") int top,
        Model model
    ) {
        return detail(
            model,
            "products",
            preset,
            fromDate,
            toDate,
            groupBy,
            threshold,
            top
        );
    }

    @GetMapping("/admin/statistics/customers")
    public String customers(
        @RequestParam(required = false) String preset,
        @RequestParam(required = false) @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        ) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        ) LocalDate toDate,
        @RequestParam(required = false) String groupBy,
        @RequestParam(defaultValue = "5") int threshold,
        @RequestParam(defaultValue = "5") int top,
        Model model
    ) {
        return detail(
            model,
            "customers",
            preset,
            fromDate,
            toDate,
            groupBy,
            threshold,
            top
        );
    }

    @GetMapping("/admin/statistics/promotions")
    public String promotions(
        @RequestParam(required = false) String preset,
        @RequestParam(required = false) @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        ) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        ) LocalDate toDate,
        @RequestParam(required = false) String groupBy,
        @RequestParam(defaultValue = "5") int threshold,
        @RequestParam(defaultValue = "5") int top,
        Model model
    ) {
        return detail(
            model,
            "promotions",
            preset,
            fromDate,
            toDate,
            groupBy,
            threshold,
            top
        );
    }

    @GetMapping("/admin/statistics/inventory")
    public String inventory(
        @RequestParam(required = false) String preset,
        @RequestParam(required = false) @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        ) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        ) LocalDate toDate,
        @RequestParam(required = false) String groupBy,
        @RequestParam(defaultValue = "5") int threshold,
        @RequestParam(defaultValue = "5") int top,
        Model model
    ) {
        return detail(
            model,
            "inventory",
            preset,
            fromDate,
            toDate,
            groupBy,
            threshold,
            top
        );
    }

    private String detail(
        Model model,
        String section,
        String preset,
        LocalDate fromDate,
        LocalDate toDate,
        String groupBy,
        int threshold,
        int top
    ) {
        addReport(
            model,
            section,
            preset,
            fromDate,
            toDate,
            groupBy,
            threshold,
            top
        );
        return "admin/statistics-detail";
    }

    private void addReport(
        Model model,
        String section,
        String preset,
        LocalDate fromDate,
        LocalDate toDate,
        String groupBy,
        int threshold,
        int top
    ) {
        model.addAttribute("section", section);
        model.addAttribute("view", viewSupport);
        model.addAttribute(
            "report",
            statistics.report(preset, fromDate, toDate, groupBy, threshold, top)
        );
    }
}
