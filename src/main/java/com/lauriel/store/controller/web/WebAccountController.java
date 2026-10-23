package com.lauriel.store.controller.web;

import com.lauriel.store.dto.request.AccountStatusRequest;
import com.lauriel.store.dto.request.ChangeRoleRequest;
import com.lauriel.store.dto.request.CreateManagementAccountRequest;
import com.lauriel.store.dto.request.ResetPasswordRequest;
import com.lauriel.store.dto.request.UpdateAccountRequest;
import com.lauriel.store.entity.enums.UserStatus;
import com.lauriel.store.security.StoreRole;
import com.lauriel.store.security.StoreUserDetails;
import com.lauriel.store.service.AccountManagementService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/accounts")
public class WebAccountController {

    private final AccountManagementService accounts;

    public WebAccountController(AccountManagementService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    public String list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) StoreRole role,
        @RequestParam(required = false) UserStatus status,
        Model model
    ) {
        model.addAttribute(
            "page",
            accounts.list(page, size, keyword, role, status)
        );
        model.addAttribute("keyword", keyword);
        model.addAttribute("role", role);
        model.addAttribute("status", status);
        model.addAttribute("accountRoles", StoreRole.values());
        return "admin/accounts/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        var account = accounts.get(id);
        model.addAttribute("account", account);
        model.addAttribute("roleForm", new ChangeRoleRequest(account.role()));
        model.addAttribute("passwordForm", new ResetPasswordRequest("", ""));
        model.addAttribute("editableRoles", editableRoles(account.role()));
        return "admin/accounts/detail";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute(
                "form",
                new CreateManagementAccountRequest(
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    StoreRole.STAFF,
                    UserStatus.ACTIVE
                )
            );
        }
        return accountForm(
            model,
            "Tạo tài khoản quản lý",
            "/admin/accounts",
            false
        );
    }

    @PostMapping
    public String create(
        @AuthenticationPrincipal StoreUserDetails actor,
        @Valid @ModelAttribute("form") CreateManagementAccountRequest request,
        BindingResult errors,
        Model model,
        RedirectAttributes flash
    ) {
        final Long[] createdId = new Long[1];
        if (
            !WebActions.save(
                () ->
                    createdId[0] = accounts
                        .create(actor.getUserId(), request)
                        .userId(),
                errors
            )
        ) {
            return accountForm(
                model,
                "Tạo tài khoản quản lý",
                "/admin/accounts",
                false
            );
        }
        flash.addFlashAttribute("successMessage", "Đã tạo tài khoản quản lý.");
        return "redirect:/admin/accounts/" + createdId[0];
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var account = accounts.get(id);
        if (!model.containsAttribute("form")) {
            model.addAttribute(
                "form",
                new UpdateAccountRequest(
                    account.fullName(),
                    account.email(),
                    account.phone(),
                    account.role(),
                    account.status()
                )
            );
        }
        model.addAttribute("account", account);
        return accountForm(
            model,
            "Chỉnh sửa tài khoản",
            "/admin/accounts/" + id,
            true
        );
    }

    @PostMapping("/{id}")
    public String update(
        @PathVariable Long id,
        @AuthenticationPrincipal StoreUserDetails actor,
        @Valid @ModelAttribute("form") UpdateAccountRequest request,
        BindingResult errors,
        Model model,
        RedirectAttributes flash
    ) {
        if (
            !WebActions.save(
                () -> accounts.update(actor.getUserId(), id, request),
                errors
            )
        ) {
            model.addAttribute("account", accounts.get(id));
            return accountForm(
                model,
                "Chỉnh sửa tài khoản",
                "/admin/accounts/" + id,
                true
            );
        }
        flash.addFlashAttribute(
            "successMessage",
            "Đã cập nhật thông tin tài khoản."
        );
        return "redirect:/admin/accounts/" + id;
    }

    @PostMapping("/{id}/status")
    public String status(
        @PathVariable Long id,
        @AuthenticationPrincipal StoreUserDetails actor,
        @Valid @ModelAttribute AccountStatusRequest request,
        BindingResult errors,
        RedirectAttributes flash
    ) {
        if (errors.hasErrors()) {
            flash.addFlashAttribute("errorMessage", "Trạng thái không hợp lệ.");
            return "redirect:/admin/accounts/" + id;
        }
        return WebActions.action(
            () -> accounts.setStatus(actor.getUserId(), id, request.status()),
            "/admin/accounts/" + id,
            flash
        );
    }

    @PostMapping("/{id}/role")
    public String role(
        @PathVariable Long id,
        @AuthenticationPrincipal StoreUserDetails actor,
        @Valid @ModelAttribute ChangeRoleRequest request,
        BindingResult errors,
        RedirectAttributes flash
    ) {
        if (errors.hasErrors()) {
            flash.addFlashAttribute("errorMessage", "Role không hợp lệ.");
            return "redirect:/admin/accounts/" + id;
        }
        return WebActions.action(
            () -> accounts.changeRole(actor.getUserId(), id, request.role()),
            "/admin/accounts/" + id,
            flash
        );
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(
        @PathVariable Long id,
        @AuthenticationPrincipal StoreUserDetails actor,
        @Valid @ModelAttribute ResetPasswordRequest request,
        BindingResult errors,
        RedirectAttributes flash
    ) {
        if (errors.hasErrors()) {
            flash.addFlashAttribute(
                "errorMessage",
                "Mật khẩu phải có từ 8 đến 72 ký tự."
            );
            return "redirect:/admin/accounts/" + id;
        }
        return WebActions.action(
            () -> accounts.resetPassword(actor.getUserId(), id, request),
            "/admin/accounts/" + id,
            flash
        );
    }

    private String accountForm(
        Model model,
        String title,
        String action,
        boolean editing
    ) {
        model.addAttribute("title", title);
        model.addAttribute("action", action);
        model.addAttribute("editing", editing);
        model.addAttribute(
            "managementRoles",
            List.of(StoreRole.ADMIN, StoreRole.STAFF)
        );
        model.addAttribute("accountStatuses", UserStatus.values());
        return "admin/accounts/form";
    }

    private List<StoreRole> editableRoles(StoreRole current) {
        return switch (current) {
            case CUSTOMER -> List.of(StoreRole.CUSTOMER);
            case SUPER_ADMIN -> List.of(
                StoreRole.SUPER_ADMIN,
                StoreRole.ADMIN,
                StoreRole.STAFF
            );
            case ADMIN, STAFF -> List.of(StoreRole.ADMIN, StoreRole.STAFF);
        };
    }
}
