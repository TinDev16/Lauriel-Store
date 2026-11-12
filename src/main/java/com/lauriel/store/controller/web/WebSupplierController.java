package com.lauriel.store.controller.web;

import com.lauriel.store.dto.request.*;
import com.lauriel.store.entity.enums.ActiveStatus;
import com.lauriel.store.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/suppliers")
public class WebSupplierController {

    private final SupplierService suppliers;
    private final com.lauriel.store.service.SupplierExcelExportService exportService;

    public WebSupplierController(
        SupplierService suppliers,
        com.lauriel.store.service.SupplierExcelExportService exportService
    ) {
        this.suppliers = suppliers;
        this.exportService = exportService;
    }

    @GetMapping("/export.xlsx")
    public void exportExcel(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) ActiveStatus status,
        jakarta.servlet.http.HttpServletResponse response
    ) throws java.io.IOException {
        String timestamp = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .format(java.time.LocalDateTime.now());
        String filename = "suppliers_" + timestamp + ".xlsx";

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(
            org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + filename + "\""
        );
        exportService.exportSuppliers(keyword, status, response.getOutputStream());
        response.flushBuffer();
    }

    @GetMapping
    public String list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) ActiveStatus status,
        Model m
    ) {
        m.addAttribute("page", suppliers.list(page, size, keyword, status));
        m.addAttribute("keyword", keyword);
        m.addAttribute("status", status);
        return "admin/suppliers/list";
    }

    @GetMapping("/new")
    public String createForm(Model m) {
        m.addAttribute(
            "form",
            new SupplierRequest("", null, null, null, ActiveStatus.ACTIVE)
        );
        return form(m, null);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model m) {
        var s = suppliers.detail(id);
        m.addAttribute(
            "form",
            new SupplierRequest(
                s.supplierName(),
                s.phone(),
                s.email(),
                s.address(),
                s.status()
            )
        );
        return form(m, id);
    }

    @PostMapping
    public String create(
        @Valid @ModelAttribute("form") SupplierRequest f,
        BindingResult e,
        Model m,
        RedirectAttributes flash
    ) {
        if (!WebActions.save(() -> suppliers.create(f), e)) return form(
            m,
            null
        );
        return WebActions.saved("/admin/suppliers", flash);
    }

    @PostMapping("/{id}")
    public String update(
        @PathVariable Long id,
        @Valid @ModelAttribute("form") SupplierRequest f,
        BindingResult e,
        Model m,
        RedirectAttributes flash
    ) {
        if (!WebActions.save(() -> suppliers.update(id, f), e)) return form(
            m,
            id
        );
        return WebActions.saved("/admin/suppliers", flash);
    }

    @PostMapping("/{id}/status")
    public String status(
        @PathVariable Long id,
        @Valid @ModelAttribute ProductStatusRequest f,
        BindingResult e,
        RedirectAttributes flash
    ) {
        if (e.hasErrors()) {
            flash.addFlashAttribute("errorMessage", "Trạng thái không hợp lệ.");
            return "redirect:/admin/suppliers";
        }
        return WebActions.action(
            () -> suppliers.updateStatus(id, f.status()),
            "/admin/suppliers",
            flash
        );
    }

    private String form(Model m, Long id) {
        m.addAttribute("id", id);
        m.addAttribute(
            "action",
            "/admin/suppliers" + (id == null ? "" : "/" + id)
        );
        return "admin/suppliers/form";
    }
}
