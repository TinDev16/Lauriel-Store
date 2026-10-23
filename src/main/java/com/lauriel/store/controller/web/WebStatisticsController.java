package com.lauriel.store.controller.web;

import com.lauriel.store.service.StatisticsService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WebStatisticsController {

    private final StatisticsService statistics;

    public WebStatisticsController(StatisticsService statistics) {
        this.statistics = statistics;
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
        model.addAttribute(
            "report",
            statistics.report(preset, fromDate, toDate, groupBy, threshold, top)
        );
    }
}
