package com.lauriel.store.controller.web;

import com.lauriel.store.dto.request.OrderStatusRequest;
import com.lauriel.store.entity.enums.OrderStatus;
import com.lauriel.store.security.StoreUserDetails;
import com.lauriel.store.service.*;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/orders")
public class WebOrderController {

    private final OrderService orders;
    private final CustomerService customers;

    public WebOrderController(OrderService orders, CustomerService customers) {
        this.orders = orders;
        this.customers = customers;
    }

    @GetMapping
    public String list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) OrderStatus status,
        @RequestParam(required = false) LocalDateTime from,
        @RequestParam(required = false) LocalDateTime to,
        Model m
    ) {
        var result = orders.staffList(page, size, keyword, status, from, to);
        m.addAttribute("page", result);
        m.addAttribute(
            "customerNames",
            customers.names(
                result
                    .content()
                    .stream()
                    .map(o -> o.customerId())
                    .toList()
            )
        );
        m.addAttribute("keyword", keyword);
        m.addAttribute("status", status);
        m.addAttribute("from", from);
        m.addAttribute("to", to);
        return "admin/orders/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model m) {
        var order = orders.staffDetail(id);
        m.addAttribute("order", order);
        m.addAttribute("customer", customers.detail(order.customerId()));
        m.addAttribute(
            "transitions",
            orders.allowedStaffTransitions(order.status())
        );
        return "admin/orders/detail";
    }

    @PostMapping("/{id}/status")
    public String status(
        @PathVariable Long id,
        @Valid @ModelAttribute OrderStatusRequest form,
        BindingResult errors,
        @AuthenticationPrincipal StoreUserDetails actor,
        RedirectAttributes flash
    ) {
        if (errors.hasErrors()) {
            flash.addFlashAttribute("errorMessage", "Trạng thái không hợp lệ.");
            return "redirect:/admin/orders/" + id;
        }
        return WebActions.action(
            () -> orders.updateStatus(id, form.status(), actor.getUserId()),
            "/admin/orders/" + id,
            flash
        );
    }
}
