package com.lauriel.store.controller.web.store;

import com.lauriel.store.dto.request.ProfileUpdateRequest;
import com.lauriel.store.exception.*;
import com.lauriel.store.security.StoreUserDetails;
import com.lauriel.store.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/shop/profile")
public class WebStoreProfileController {
    private final CustomerService customers;
    public WebStoreProfileController(CustomerService customers) { this.customers = customers; }

    @GetMapping
    public String profile(@AuthenticationPrincipal StoreUserDetails user, Model model) {
        model.addAttribute("profile", customers.me(user.getUserId())); return "store/profile";
    }

    @GetMapping("/edit")
    public String edit(@AuthenticationPrincipal StoreUserDetails user, Model model) {
        var profile = customers.me(user.getUserId());
        model.addAttribute("profile", profile);
        model.addAttribute("form", new ProfileUpdateRequest(profile.fullName(), profile.email(), profile.phone(), profile.address()));
        return "store/profile-edit";
    }

    @PostMapping("/edit")
    public String update(@AuthenticationPrincipal StoreUserDetails user,
            @Valid @ModelAttribute("form") ProfileUpdateRequest form, BindingResult errors,
            Model model, RedirectAttributes flash) {
        if (errors.hasErrors()) { model.addAttribute("profile", customers.me(user.getUserId())); return "store/profile-edit"; }
        try { customers.updateMe(user.getUserId(), form); flash.addFlashAttribute("successMessage", "Đã cập nhật hồ sơ."); return "redirect:/shop/profile"; }
        catch (BadRequestException | DuplicateResourceException exception) {
            errors.reject("business", exception.getMessage()); model.addAttribute("profile", customers.me(user.getUserId())); return "store/profile-edit";
        }
    }
}
