package com.lauriel.store.controller.web.store;

import com.lauriel.store.dto.store.CartItemForm;
import com.lauriel.store.exception.*;
import com.lauriel.store.security.StoreUserDetails;
import com.lauriel.store.service.StoreCartService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
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

    public WebStoreCartController(StoreCartService carts) {
        this.carts = carts;
    }

    @GetMapping
    public String cart(HttpSession session, Model model) {
        model.addAttribute("cart", carts.view(session, false));
        return "store/cart";
    }

    @PostMapping("/add")
    public String add(
        @Valid @ModelAttribute CartItemForm form,
        BindingResult errors,
        @RequestParam Long productId,
        @AuthenticationPrincipal StoreUserDetails user,
        HttpSession session,
        RedirectAttributes flash
    ) {
        if (errors.hasErrors()) return error(
            flash,
            "Sản phẩm hoặc số lượng không hợp lệ.",
            productId
        );
        try {
            carts.add(session, form.variantId(), form.quantity(), userId(user));
            flash.addFlashAttribute("successMessage", "Đã thêm vào giỏ hàng.");
        } catch (BadRequestException | ResourceNotFoundException exception) {
            flash.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/shop/products/" + productId;
    }

    @PostMapping("/buy-now")
    public String buyNow(
        @Valid @ModelAttribute CartItemForm form,
        BindingResult errors,
        @RequestParam Long productId,
        HttpSession session,
        RedirectAttributes flash
    ) {
        if (errors.hasErrors()) return error(
            flash,
            "Sản phẩm hoặc số lượng không hợp lệ.",
            productId
        );
        try {
            carts.buyNow(session, form.variantId(), form.quantity());
            return "redirect:/shop/checkout?mode=buy-now";
        } catch (BadRequestException | ResourceNotFoundException exception) {
            flash.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/shop/products/" + productId;
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
