package com.lauriel.store.controller.web;

import com.lauriel.store.dto.request.CategoryRequest;
import com.lauriel.store.dto.request.ProductStatusRequest;
import com.lauriel.store.entity.enums.ActiveStatus;
import com.lauriel.store.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
public class WebCategoryController {

    private final CategoryService service;

    public WebCategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public String list(
        @RequestParam(required = false) ActiveStatus status,
        Model model
    ) {
        ActiveStatus effectiveStatus = (status != null) ? status : ActiveStatus.ACTIVE;
        model.addAttribute("items", service.listAdmin(effectiveStatus));
        model.addAttribute("effectiveStatus", effectiveStatus);
        model.addAttribute("status", status);
        model.addAttribute("activeCount", service.countByStatus(ActiveStatus.ACTIVE));
        model.addAttribute("inactiveCount", service.countByStatus(ActiveStatus.INACTIVE));
        return "admin/categories/list";
    }

    @GetMapping("/inactive")
    public String listInactive(Model model) {
        return list(ActiveStatus.INACTIVE, model);
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute(
            "form",
            new CategoryRequest("", ActiveStatus.ACTIVE)
        );
        return form(model, null);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        var item = service.detail(id);
        model.addAttribute(
            "form",
            new CategoryRequest(item.categoryName(), item.status())
        );
        return form(model, id);
    }

    @PostMapping
    public String create(
        @Valid @ModelAttribute("form") CategoryRequest request,
        BindingResult errors,
        Model model,
        RedirectAttributes flash
    ) {
        if (
            !WebActions.save(() -> service.create(request), errors)
        ) return form(model, null);
        return WebActions.saved("/admin/categories", flash);
    }

    @PostMapping("/{id}")
    public String update(
        @PathVariable Long id,
        @Valid @ModelAttribute("form") CategoryRequest request,
        BindingResult errors,
        Model model,
        RedirectAttributes flash
    ) {
        if (
            !WebActions.save(() -> service.update(id, request), errors)
        ) return form(model, id);
        return WebActions.saved("/admin/categories", flash);
    }

    @PostMapping("/{id}/status")
    public String status(
        @PathVariable Long id,
        @Valid @ModelAttribute ProductStatusRequest request,
        BindingResult errors,
        @RequestParam(required = false) String returnUrl,
        RedirectAttributes flash
    ) {
        if (errors.hasErrors()) {
            flash.addFlashAttribute("errorMessage", "Trạng thái không hợp lệ.");
            return "redirect:/admin/categories";
        }
        String redirect = (returnUrl != null && !returnUrl.isBlank()) ? returnUrl : "/admin/categories";
        return WebActions.action(
            () -> service.updateStatus(id, request.status()),
            redirect,
            flash
        );
    }

    private String form(Model model, Long id) {
        model.addAttribute("id", id);
        model.addAttribute(
            "action",
            id == null ? "/admin/categories" : "/admin/categories/" + id
        );
        return "admin/categories/form";
    }
}
