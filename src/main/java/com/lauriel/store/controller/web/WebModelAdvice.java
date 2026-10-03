package com.lauriel.store.controller.web;

import com.lauriel.store.entity.enums.ActiveStatus;
import com.lauriel.store.entity.enums.DiscountType;
import com.lauriel.store.entity.enums.InventoryTransactionType;
import com.lauriel.store.entity.enums.OrderStatus;
import com.lauriel.store.entity.enums.PurchaseReceiptStatus;
import com.lauriel.store.entity.enums.UserStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "com.lauriel.store.controller.web")
public class WebModelAdvice {
    @InitBinder
    void bind(WebDataBinder binder) {
        // Bind validated record constructor arguments; no entity/property mass assignment.
        binder.setDeclarativeBinding(true);
    }

    @ModelAttribute("isAdmin")
    boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    @ModelAttribute("username")
    String username(Authentication auth) {
        return auth == null ? "" : auth.getName();
    }

    @ModelAttribute("statuses")
    ActiveStatus[] statuses() { return ActiveStatus.values(); }

    @ModelAttribute("orderStatuses")
    OrderStatus[] orderStatuses() { return OrderStatus.values(); }

    @ModelAttribute("receiptStatuses")
    PurchaseReceiptStatus[] receiptStatuses() { return PurchaseReceiptStatus.values(); }

    @ModelAttribute("userStatuses")
    UserStatus[] userStatuses() { return UserStatus.values(); }

    @ModelAttribute("discountTypes")
    DiscountType[] discountTypes() { return DiscountType.values(); }

    @ModelAttribute("transactionTypes")
    InventoryTransactionType[] transactionTypes() { return InventoryTransactionType.values(); }
}
