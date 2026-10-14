package com.lauriel.store.controller.web.store;

import com.lauriel.store.dto.request.CheckoutRequest;
import com.lauriel.store.dto.request.OrderPreviewRequest;
import com.lauriel.store.dto.store.StoreCheckoutForm;
import com.lauriel.store.exception.*;
import com.lauriel.store.security.StoreUserDetails;
import com.lauriel.store.service.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/shop/checkout")
public class WebStoreCheckoutController {
    private final StoreCartService carts;
    private final VoucherService vouchers;
    private final OrderService orders;
    private final CustomerService customers;

    public WebStoreCheckoutController(StoreCartService carts, VoucherService vouchers,
            OrderService orders, CustomerService customers) {
        this.carts = carts; this.vouchers = vouchers; this.orders = orders; this.customers = customers;
    }

    @GetMapping
    public String checkout(@RequestParam(defaultValue = "cart") String mode,
            @AuthenticationPrincipal StoreUserDetails user, HttpSession session, Model model,
            RedirectAttributes flash) {
        boolean buyNow = "buy-now".equals(mode);
        var items = carts.checkoutItems(session, buyNow);
        if (items.isEmpty()) {
            flash.addFlashAttribute("errorMessage", buyNow ? "Sản phẩm mua ngay không còn khả dụng." : "Giỏ hàng đang trống.");
            return "redirect:/shop/cart";
        }
        String selected = carts.selectedVoucher(session);
        com.lauriel.store.dto.response.VoucherValidationResponse pricing;
        try { pricing = vouchers.preview(new OrderPreviewRequest(items, selected)); }
        catch (BadRequestException | ResourceNotFoundException exception) {
            carts.selectVoucher(session, null);
            pricing = vouchers.preview(new OrderPreviewRequest(items, null));
            model.addAttribute("errorMessage", exception.getMessage());
        }
        var profile = customers.me(user.getUserId());
        model.addAttribute("cart", carts.view(session, buyNow));
        model.addAttribute("pricing", pricing);
        model.addAttribute("vouchers", vouchers.listAvailable(0, 20).content());
        model.addAttribute("selectedVoucher", carts.selectedVoucher(session));
        model.addAttribute("mode", buyNow ? "buy-now" : "cart");
        model.addAttribute("profile", profile);
        model.addAttribute("form", new StoreCheckoutForm(profile.address(), carts.selectedVoucher(session),
                buyNow ? "buy-now" : "cart"));
        return "store/checkout";
    }

    @PostMapping("/voucher")
    public String voucher(@RequestParam(required = false) String code,
            @RequestParam(defaultValue = "cart") String mode, HttpSession session, RedirectAttributes flash) {
        boolean buyNow = "buy-now".equals(mode);
        try {
            var preview = vouchers.preview(new OrderPreviewRequest(carts.checkoutItems(session, buyNow), code));
            carts.selectVoucher(session, preview.code());
            flash.addFlashAttribute("successMessage", "Đã áp dụng mã " + preview.code() + ".");
        } catch (BadRequestException | ResourceNotFoundException exception) {
            flash.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/shop/checkout?mode=" + (buyNow ? "buy-now" : "cart");
    }

    @PostMapping("/voucher/remove")
    public String removeVoucher(@RequestParam(defaultValue = "cart") String mode,
            HttpSession session, RedirectAttributes flash) {
        carts.selectVoucher(session, null); flash.addFlashAttribute("successMessage", "Đã bỏ mã giảm giá.");
        return "redirect:/shop/checkout?mode=" + ("buy-now".equals(mode) ? "buy-now" : "cart");
    }

    @PostMapping
    public String submit(@Valid @ModelAttribute("form") StoreCheckoutForm form, BindingResult errors,
            @AuthenticationPrincipal StoreUserDetails user, HttpSession session, Model model,
            RedirectAttributes flash) {
        boolean buyNow = "buy-now".equals(form.mode());
        if (errors.hasErrors()) {
            flash.addFlashAttribute("errorMessage", "Vui lòng kiểm tra địa chỉ giao hàng.");
            return "redirect:/shop/checkout?mode=" + (buyNow ? "buy-now" : "cart");
        }
        try {
            var items = carts.checkoutItems(session, buyNow);
            var order = orders.checkout(new CheckoutRequest(items, form.shippingAddress(),
                    carts.selectedVoucher(session)), user.getUserId());
            if (buyNow) carts.clearBuyNow(session); else carts.clear(session);
            return "redirect:/shop/orders/" + order.orderId() + "/success";
        } catch (BadRequestException | ResourceConflictException | ResourceNotFoundException exception) {
            flash.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/shop/checkout?mode=" + (buyNow ? "buy-now" : "cart");
        }
    }
}
