package com.lauriel.store.controller.web.store;

import com.lauriel.store.dto.request.CheckoutRequest;
import com.lauriel.store.dto.request.OrderPreviewRequest;
import com.lauriel.store.dto.store.StoreAddressConfirmationResponse;
import com.lauriel.store.dto.store.StoreCheckoutForm;
import com.lauriel.store.entity.enums.SalesChannel;
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
    private static final String CHECKOUT_ADDRESS_SESSION_PREFIX = "store.checkout.address.";

    private final StoreCartService carts;
    private final VoucherService vouchers;
    private final OrderService orders;
    private final CustomerService customers;
    private final CustomerAddressService addresses;

    public WebStoreCheckoutController(StoreCartService carts, VoucherService vouchers,
            OrderService orders, CustomerService customers, CustomerAddressService addresses) {
        this.carts = carts; this.vouchers = vouchers; this.orders = orders; this.customers = customers;
        this.addresses = addresses;
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
        var savedAddresses = addresses.listForCheckout(user.getUserId());
        String defaultAddress = savedAddresses.stream().filter(com.lauriel.store.dto.store.StoreCustomerAddress::defaultAddress)
                .map(com.lauriel.store.dto.store.StoreCustomerAddress::address).findFirst().orElse(profile.address());
        String rememberedAddress = rememberedAddress(session, user.getUserId());
        String checkoutAddress = rememberedAddress != null ? rememberedAddress : defaultAddress;
        boolean checkoutAddressSaved = checkoutAddress != null && savedAddresses.stream()
                .anyMatch(address -> address.address().equalsIgnoreCase(checkoutAddress.strip()));
        model.addAttribute("cart", carts.view(session, buyNow));
        model.addAttribute("pricing", pricing);
        model.addAttribute("vouchers", vouchers.listAvailable(0, 20).content());
        model.addAttribute("selectedVoucher", carts.selectedVoucher(session));
        model.addAttribute("mode", buyNow ? "buy-now" : "cart");
        model.addAttribute("profile", profile);
        model.addAttribute("savedAddresses", savedAddresses);
        model.addAttribute("addressConfirmed", checkoutAddress != null && !checkoutAddress.isBlank());
        model.addAttribute("addressSaved", checkoutAddressSaved);
        model.addAttribute("form", new StoreCheckoutForm(checkoutAddress, carts.selectedVoucher(session),
                buyNow ? "buy-now" : "cart", false, false));
        return "store/checkout";
    }

    @PostMapping("/address/confirm")
    @ResponseBody
    public StoreAddressConfirmationResponse confirmAddress(@RequestParam String shippingAddress,
            @RequestParam(defaultValue = "false") boolean saveAddress,
            @RequestParam(defaultValue = "false") boolean setAsDefault,
            @AuthenticationPrincipal StoreUserDetails user, HttpSession session) {
        String normalized = shippingAddress == null ? "" : shippingAddress.strip();
        if (normalized.isEmpty()) throw new BadRequestException("Vui lòng nhập địa chỉ nhận hàng.");
        if (normalized.length() > 10000) throw new BadRequestException("Địa chỉ nhận hàng không được vượt quá 10.000 ký tự.");

        session.setAttribute(addressSessionKey(user.getUserId()), normalized);
        if (saveAddress || setAsDefault) {
            addresses.saveFromCheckout(user.getUserId(), normalized, setAsDefault);
        }
        return new StoreAddressConfirmationResponse(normalized, saveAddress || setAsDefault,
                addresses.listForCheckout(user.getUserId()));
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
        session.setAttribute(addressSessionKey(user.getUserId()), form.shippingAddress().strip());
        try {
            var items = carts.checkoutItems(session, buyNow);
            if (Boolean.TRUE.equals(form.saveAddress())) {
                addresses.saveFromCheckout(user.getUserId(), form.shippingAddress(),
                        Boolean.TRUE.equals(form.setAsDefault()));
            }
            var order = orders.checkout(new CheckoutRequest(items, form.shippingAddress(),
                    carts.selectedVoucher(session)), user.getUserId(), SalesChannel.WEB);
            session.removeAttribute(addressSessionKey(user.getUserId()));
            if (buyNow) carts.clearBuyNow(session); else carts.clear(session, user.getUserId());
            return "redirect:/shop/orders/" + order.orderId() + "/success";
        } catch (BadRequestException | ResourceConflictException | ResourceNotFoundException exception) {
            flash.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/shop/checkout?mode=" + (buyNow ? "buy-now" : "cart");
        }
    }

    private static String rememberedAddress(HttpSession session, Long userId) {
        Object value = session.getAttribute(addressSessionKey(userId));
        return value instanceof String address && !address.isBlank() ? address : null;
    }

    private static String addressSessionKey(Long userId) {
        return CHECKOUT_ADDRESS_SESSION_PREFIX + userId;
    }
}
