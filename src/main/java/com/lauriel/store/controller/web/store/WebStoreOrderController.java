package com.lauriel.store.controller.web.store;

import com.lauriel.store.entity.enums.OrderStatus;
import com.lauriel.store.exception.*;
import com.lauriel.store.repository.ProductVariantRepository;
import com.lauriel.store.security.StoreUserDetails;
import com.lauriel.store.service.OrderService;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/shop/orders")
public class WebStoreOrderController {

    private final OrderService orders;
    private final ProductVariantRepository variants;

    public WebStoreOrderController(
        OrderService orders,
        ProductVariantRepository variants
    ) {
        this.orders = orders;
        this.variants = variants;
    }

    @GetMapping
    public String list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(required = false) OrderStatus status,
        @AuthenticationPrincipal StoreUserDetails user,
        Model model
    ) {
        model.addAttribute(
            "page",
            orders.myOrders(user.getUserId(), page, 10, status)
        );
        model.addAttribute("selectedStatus", status);
        return "store/orders";
    }

    @GetMapping("/{id}")
    public String detail(
        @PathVariable Long id,
        @AuthenticationPrincipal StoreUserDetails user,
        Model model
    ) {
        addOrder(model, orders.myDetail(id, user.getUserId()));
        return "store/order-detail";
    }

    @GetMapping("/{id}/success")
    public String success(
        @PathVariable Long id,
        @AuthenticationPrincipal StoreUserDetails user,
        Model model
    ) {
        addOrder(model, orders.myDetail(id, user.getUserId()));
        return "store/checkout-success";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(
        @PathVariable Long id,
        @AuthenticationPrincipal StoreUserDetails user,
        RedirectAttributes flash
    ) {
        try {
            orders.customerCancel(id, user.getUserId());
            flash.addFlashAttribute("successMessage", "Đã hủy đơn hàng.");
        } catch (
            BadRequestException
            | ResourceConflictException
            | ResourceNotFoundException exception
        ) {
            flash.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/shop/orders/" + id;
    }

    private void addOrder(
        Model model,
        com.lauriel.store.dto.response.OrderResponse order
    ) {
        model.addAttribute("order", order);
        Map<Long, Long> productIds = variants
            .findByVariantIdIn(
                order
                    .items()
                    .stream()
                    .map(item -> item.variantId())
                    .toList()
            )
            .stream()
            .collect(
                Collectors.toMap(
                    variant -> variant.getVariantId(),
                    variant -> variant.getProduct().getProductId()
                )
            );
        model.addAttribute("productIds", productIds);
        model.addAttribute(
            "cancellable",
            order.status() == OrderStatus.PENDING ||
                order.status() == OrderStatus.CONFIRMED
        );
    }
}
