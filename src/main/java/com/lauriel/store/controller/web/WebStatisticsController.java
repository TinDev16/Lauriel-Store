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
    public String dashboard(@RequestParam(required = false) String preset,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                            LocalDate fromDate,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                            LocalDate toDate,
                            @RequestParam(required = false) String groupBy,
                            @RequestParam(defaultValue = "5") int threshold,
                            @RequestParam(defaultValue = "5") int top,
                            Model model) {
        model.addAttribute("report", statistics.report(preset, fromDate, toDate, groupBy, threshold, top));
        return "admin/statistics";
    }
}