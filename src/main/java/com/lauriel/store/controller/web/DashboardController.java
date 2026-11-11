package com.lauriel.store.controller.web;

import com.lauriel.store.service.DashboardService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping({ "/admin", "/admin/dashboard" })
    public String dashboard(Model model) {
        var today = LocalDate.now();
        model.addAttribute("summary", dashboard.summary());
        model.addAttribute(
            "todayFrom",
            today.atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
        );
        model.addAttribute(
            "todayTo",
            today.atTime(23, 59).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
        );
        return "admin/dashboard";
    }
}
