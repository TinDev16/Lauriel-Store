package com.lauriel.store.controller.web;

import com.lauriel.store.dto.request.*;
import com.lauriel.store.entity.enums.ActiveStatus;
import com.lauriel.store.service.OrderPromotionService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/order-promotions")
public class WebOrderPromotionController {

    private final OrderPromotionService service;

    public WebOrderPromotionController(OrderPromotionService service) {
        this.service = service;
    }

    @GetMapping
    public String list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) ActiveStatus status
    ) {
        String query = "?tab=order-promotions";
        if (page > 0) query += "&page=" + page;
        if (size != 12) query += "&size=" + size;
        if (keyword != null && !keyword.isBlank()) {
            query += "&keyword=" + java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8);
        }
        if (status != null) query += "&status=" + status.name();
        return "redirect:/admin/promotions/overview" + query;
    }

    @GetMapping("/new")
    public String create(Model m) {
        m.addAttribute(
            "form",
            new OrderPromotionRequest(
                "",
                null,
                null,
                null,
                null,
                null,
                null,
                ActiveStatus.ACTIVE
            )
        );
        return form(m, null);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model m) {
        var v = service.detail(id);
        m.addAttribute(
            "form",
            new OrderPromotionRequest(
                v.promotionName(),
                v.description(),
                v.minOrderAmount(),
                v.discountPercent(),
                v.maxDiscountAmount(),
                v.startDate(),
                v.endDate(),
                v.status()
            )
        );
        return form(m, id);
    }

    @PostMapping
    public String create(
        @Valid @ModelAttribute("form") OrderPromotionRequest r,
        BindingResult e,
        Model m,
        RedirectAttributes f
    ) {
        if (!WebActions.save(() -> service.create(r), e)) return form(m, null);
        return WebActions.saved("/admin/promotions/overview?tab=order-promotions", f);
    }

    @PostMapping("/{id}")
    public String update(
        @PathVariable Long id,
        @Valid @ModelAttribute("form") OrderPromotionRequest r,
        BindingResult e,
        Model m,
        RedirectAttributes f
    ) {
        if (!WebActions.save(() -> service.update(id, r), e)) return form(
            m,
            id
        );
        return WebActions.saved("/admin/promotions/overview?tab=order-promotions", f);
    }

    @PostMapping("/{id}/status")
    public String status(
        @PathVariable Long id,
        @RequestParam ActiveStatus status,
        RedirectAttributes f
    ) {
        return WebActions.action(
            () -> service.updateStatus(id, status),
            "/admin/promotions/overview?tab=order-promotions",
            f
        );
    }

    private String form(Model m, Long id) {
        m.addAttribute("id", id);
        m.addAttribute(
            "action",
            "/admin/order-promotions" + (id == null ? "" : "/" + id)
        );
        return "admin/order-promotions/form";
    }
}
