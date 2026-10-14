package com.lauriel.store.controller.web.store;

import com.lauriel.store.service.StoreCartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "com.lauriel.store.controller.web.store")
public class StoreModelAdvice {
    private final StoreCartService carts;
    public StoreModelAdvice(StoreCartService carts) { this.carts = carts; }

    @ModelAttribute("cartCount")
    int cartCount(HttpSession session) { return carts.count(session); }

    @ModelAttribute("storeLoggedIn")
    boolean loggedIn(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));
    }
}
