package com.lauriel.store.controller.web;

import com.lauriel.store.exception.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

final class WebActions {

    private WebActions() {}

    static String action(
        Runnable action,
        String location,
        RedirectAttributes flash
    ) {
        try {
            action.run();
            flash.addFlashAttribute("successMessage", "Đã lưu thành công.");
        } catch (
            BadRequestException
            | ResourceConflictException
            | DuplicateResourceException e
        ) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            flash.addFlashAttribute(
                "errorMessage",
                "Dữ liệu trùng với bản ghi đã có."
            );
        }
        return "redirect:" + location;
    }

    static boolean save(Runnable action, BindingResult errors) {
        if (errors.hasErrors()) return false;
        try {
            action.run();
            return true;
        } catch (
            BadRequestException
            | ResourceConflictException
            | DuplicateResourceException e
        ) {
            errors.reject("business", e.getMessage());
            return false;
        } catch (DataIntegrityViolationException e) {
            errors.reject("conflict", "Dữ liệu trùng với bản ghi đã có.");
            return false;
        }
    }

    static String saved(String location, RedirectAttributes flash) {
        flash.addFlashAttribute("successMessage", "Đã lưu thành công.");
        return "redirect:" + location;
    }
}
