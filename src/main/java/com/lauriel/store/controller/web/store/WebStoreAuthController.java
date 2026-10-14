package com.lauriel.store.controller.web.store;

import com.lauriel.store.dto.request.RegisterRequest;
import com.lauriel.store.exception.DuplicateResourceException;
import com.lauriel.store.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WebStoreAuthController {
    private final AuthService auth;
    public WebStoreAuthController(AuthService auth) { this.auth = auth; }

    @GetMapping("/register")
    public String register(Model model) {
        if (!model.containsAttribute("form")) model.addAttribute("form", new RegisterRequest("", null, "", "", null));
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterRequest form, BindingResult errors,
            RedirectAttributes flash) {
        if (errors.hasErrors()) return "auth/register";
        try { auth.registerCustomer(form); }
        catch (DuplicateResourceException exception) { errors.reject("business", exception.getMessage()); return "auth/register"; }
        flash.addFlashAttribute("successMessage", "Đăng ký thành công. Bạn có thể đăng nhập ngay.");
        return "redirect:/login";
    }
}
