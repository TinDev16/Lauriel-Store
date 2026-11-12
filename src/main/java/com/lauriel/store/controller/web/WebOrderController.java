package com.lauriel.store.controller.web;

import com.lauriel.store.dto.request.OrderStatusRequest;
import com.lauriel.store.entity.enums.OrderStatus;
import com.lauriel.store.security.StoreUserDetails;
import com.lauriel.store.service.*;
import jakarta.validation.Valid;
import java.math.BigDecimal;
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
        @RequestParam(required = false) Long customerId,
        Model m
    ) {
        var result = orders.staffList(
            page,
            size,
            keyword,
            status,
            from,
            to,
            customerId
        );
        var pageTotalAmount = result
            .content()
            .stream()
            .map(o -> o.finalAmount() != null ? o.finalAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        m.addAttribute("page", result);
        m.addAttribute("pageTotalAmount", pageTotalAmount);
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
        m.addAttribute("customerId", customerId);
        if (customerId != null) {
            try {
                m.addAttribute("filterCustomer", customers.detail(customerId));
            } catch (Exception ignored) {
                // Customer details optional for filter badge
            }
        }
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

    @GetMapping("/{id}/preview")
    public String preview(@PathVariable Long id, Model m) {
        var order = orders.staffDetail(id);
        m.addAttribute("order", order);
        m.addAttribute("customer", customers.detail(order.customerId()));
        m.addAttribute(
            "transitions",
            orders.allowedStaffTransitions(order.status())
        );
        return "admin/orders/preview";
    }

    @PostMapping("/{id}/status")
    public Object status(
        @PathVariable Long id,
        @Valid @ModelAttribute OrderStatusRequest form,
        BindingResult errors,
        @AuthenticationPrincipal StoreUserDetails actor,
        @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
        RedirectAttributes flash
    ) {
        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            if (errors.hasErrors()) {
                return org.springframework.http.ResponseEntity.badRequest()
                    .body(java.util.Map.of("success", false, "message", "Trạng thái không hợp lệ."));
            }
            try {
                var updated = orders.updateStatus(id, form.status(), actor.getUserId());
                return org.springframework.http.ResponseEntity.ok(
                    java.util.Map.of(
                        "success", true,
                        "message", "Đã cập nhật trạng thái đơn thành công.",
                        "status", updated.status().name()
                    )
                );
            } catch (Exception e) {
                return org.springframework.http.ResponseEntity.badRequest()
                    .body(java.util.Map.of("success", false, "message", e.getMessage()));
            }
        }
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
