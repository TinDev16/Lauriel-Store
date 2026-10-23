package com.lauriel.store.controller.web;

import com.lauriel.store.service.DashboardService;
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
        model.addAttribute("summary", dashboard.summary());
        return "admin/dashboard";
    }
}
