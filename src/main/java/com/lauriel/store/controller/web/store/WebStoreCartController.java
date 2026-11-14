package com.lauriel.store.controller.web.store;

import com.lauriel.store.controller.web.WebViewSupport;
import com.lauriel.store.dto.store.CartItemForm;
import com.lauriel.store.exception.*;
import com.lauriel.store.security.StoreUserDetails;
import com.lauriel.store.service.PricingService;
import com.lauriel.store.service.StoreCartService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/shop/cart")
public class WebStoreCartController {

    private final StoreCartService carts;
    private final PricingService pricing;
    private final WebViewSupport view;

    public WebStoreCartController(
        StoreCartService carts,
        PricingService pricing,
        WebViewSupport view
    ) {
        this.carts = carts;
        this.pricing = pricing;
        this.view = view;
    }

    @GetMapping
    public String cart(HttpSession session, Model model) {
        var cartView = carts.view(session, false);
        model.addAttribute("cart", cartView);
        if (cartView.totalQuantity() > 0) {
            model.addAttribute(
                "orderPromotionSuggestion",
                pricing.nextOrderPromotionSuggestion(cartView.subtotal())
            );
        }
        return "store/cart";
    }

    @PostMapping("/add")
    public Object add(
        @Valid @ModelAttribute CartItemForm form,
        BindingResult errors,
        @RequestParam Long productId,
        @AuthenticationPrincipal StoreUserDetails user,
        HttpSession session,
        RedirectAttributes flash,
        @RequestHeader(value = "Accept", defaultValue = "") String accept,
        @RequestHeader(value = "X-Requested-With", defaultValue = "") String requestedWith,
        @RequestHeader(value = "Referer", required = false) String referer
    ) {
        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(requestedWith) || accept.contains("application/json");
        if (errors.hasErrors()) {
            if (isAjax) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Sản phẩm hoặc số lượng không hợp lệ."
                ));
            }
            return error(
                flash,
                "Sản phẩm hoặc số lượng không hợp lệ.",
                productId
            );
        }
        try {
            if (form.variantId() != null) {
                carts.add(session, form.variantId(), form.quantity(), userId(user));
            } else {
                carts.addProduct(session, productId, form.quantity(), userId(user));
            }
            var cartView = carts.view(session, false);
            String successMsg = "Đã thêm vào giỏ hàng thành công!";
            var suggestion = pricing.nextOrderPromotionSuggestion(cartView.subtotal());
            if (suggestion != null) {
                successMsg = "Đã thêm vào giỏ hàng thành công! Mua thêm " + view.money(suggestion.remainingAmount()) +
                    " để nhận ưu đãi \"" + suggestion.promotionName() +
                    "\" (giảm " + view.number(suggestion.discountPercent()) + "%).";
            }
            if (isAjax) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", successMsg,
                    "cartCount", cartView.totalQuantity()
                ));
            }
            flash.addFlashAttribute("successMessage", successMsg);
        } catch (BadRequestException | ResourceNotFoundException exception) {
            if (isAjax) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", exception.getMessage()
                ));
            }
            flash.addFlashAttribute("errorMessage", exception.getMessage());
        }
        String redirectTarget = (referer != null && !referer.isBlank()) ? referer : ("/shop/products/" + productId);
        return "redirect:" + redirectTarget;
    }

    @PostMapping("/buy-now")
    public String buyNow(
        @Valid @ModelAttribute CartItemForm form,
        BindingResult errors,
        @RequestParam Long productId,
        HttpSession session,
        RedirectAttributes flash,
        @RequestHeader(value = "Referer", required = false) String referer
    ) {
        if (errors.hasErrors()) return error(
            flash,
            "Sản phẩm hoặc số lượng không hợp lệ.",
            productId
        );
        try {
            if (form.variantId() != null) {
                carts.buyNow(session, form.variantId(), form.quantity());
            } else {
                carts.buyNowProduct(session, productId, form.quantity());
            }
            return "redirect:/shop/checkout?mode=buy-now";
        } catch (BadRequestException | ResourceNotFoundException exception) {
            flash.addFlashAttribute("errorMessage", exception.getMessage());
            String redirectTarget = (referer != null && !referer.isBlank()) ? referer : ("/shop/products/" + productId);
            return "redirect:" + redirectTarget;
        }
    }

    @PostMapping("/update")
    public String update(
        @Valid @ModelAttribute CartItemForm form,
        BindingResult errors,
        @AuthenticationPrincipal StoreUserDetails user,
        HttpSession session,
        RedirectAttributes flash
    ) {
        if (errors.hasErrors()) flash.addFlashAttribute(
            "errorMessage",
            "Số lượng không hợp lệ."
        );
        else try {
            carts.update(
                session,
                form.variantId(),
                form.quantity(),
                userId(user)
            );
            flash.addFlashAttribute("successMessage", "Đã cập nhật giỏ hàng.");
        } catch (BadRequestException | ResourceNotFoundException exception) {
            flash.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/shop/cart";
    }

    @PostMapping("/remove")
    public String remove(
        @RequestParam Long variantId,
        @AuthenticationPrincipal StoreUserDetails user,
        HttpSession session,
        RedirectAttributes flash
    ) {
        carts.remove(session, variantId, userId(user));
        flash.addFlashAttribute(
            "successMessage",
            "Đã xóa sản phẩm khỏi giỏ hàng."
        );
        return "redirect:/shop/cart";
    }

    @PostMapping("/clear")
    public String clear(
        @AuthenticationPrincipal StoreUserDetails user,
        HttpSession session,
        RedirectAttributes flash
    ) {
        carts.clear(session, userId(user));
        flash.addFlashAttribute("successMessage", "Đã xóa toàn bộ giỏ hàng.");
        return "redirect:/shop/cart";
    }

    private String error(
        RedirectAttributes flash,
        String message,
        Long productId
    ) {
        flash.addFlashAttribute("errorMessage", message);
        return "redirect:/shop/products/" + productId;
    }

    private static Long userId(StoreUserDetails user) {
        return user == null ? null : user.getUserId();
    }
}
