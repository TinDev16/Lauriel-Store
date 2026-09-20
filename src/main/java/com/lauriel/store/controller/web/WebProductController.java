package com.lauriel.store.controller.web;

import com.lauriel.store.dto.request.ProductRequest;
import com.lauriel.store.dto.request.ProductStatusRequest;
import com.lauriel.store.entity.enums.ActiveStatus;
import com.lauriel.store.exception.BadRequestException;
import com.lauriel.store.service.ProductImageStorage;
import com.lauriel.store.service.ProductService;
import com.lauriel.store.service.ProductVariantService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/products")
public class WebProductController {
    private final ProductService products;
    private final ProductVariantService variants;
    private final ProductImageStorage images;

    public WebProductController(ProductService products, ProductVariantService variants, ProductImageStorage images) {
        this.products = products;
        this.variants = variants;
        this.images = images;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword, @RequestParam(required = false) ActiveStatus status,
            Model model) {
        var result = products.listAdmin(page, size, keyword, status);
        model.addAttribute("page", result);
        model.addAttribute("counts", variants.counts(result.content().stream().map(p -> p.productId()).toList()));
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        return "admin/products/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new ProductRequest("", null, null, null, ActiveStatus.ACTIVE));
        return form(model, null);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        var product = products.getById(id);
        model.addAttribute("form", new ProductRequest(product.productName(), product.description(),
                product.price(), product.imgUrl(), product.status()));
        return form(model, id);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") ProductRequest form, BindingResult errors,
            @RequestParam(name = "imageFile", required = false) MultipartFile imageFile,
            Model model, RedirectAttributes flash) {
        if (errors.hasErrors()) return form(model, null);
        String imageUrl;
        try {
            imageUrl = images.store(imageFile);
        } catch (BadRequestException exception) {
            errors.reject("image", exception.getMessage());
            return form(model, null);
        }
        var request = withImage(form, imageUrl);
        boolean saved = false;
        try {
            saved = WebActions.save(() -> products.create(request), errors);
            if (!saved) return form(model, null);
            return WebActions.saved("/admin/products", flash);
        } finally {
            if (!saved) images.deleteIfManaged(imageUrl);
        }
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") ProductRequest form,
            BindingResult errors, @RequestParam(name = "imageFile", required = false) MultipartFile imageFile,
            Model model, RedirectAttributes flash) {
        if (errors.hasErrors()) return form(model, id);
        String previousUrl = products.getById(id).imgUrl();
        String newUrl;
        try {
            newUrl = images.store(imageFile);
        } catch (BadRequestException exception) {
            errors.reject("image", exception.getMessage());
            return form(model, id);
        }
        var request = withImage(form, newUrl == null ? previousUrl : newUrl);
        boolean saved = false;
        try {
            saved = WebActions.save(() -> products.update(id, request), errors);
            if (!saved) return form(model, id);
        } finally {
            if (!saved) images.deleteIfManaged(newUrl);
        }
        if (newUrl != null) images.deleteIfManaged(previousUrl);
        return WebActions.saved("/admin/products", flash);
    }

    @PostMapping("/{id}/status")
    public String status(@PathVariable Long id, @Valid @ModelAttribute ProductStatusRequest form,
            BindingResult errors, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            flash.addFlashAttribute("errorMessage", "Trạng thái không hợp lệ.");
            return "redirect:/admin/products";
        }
        return WebActions.action(() -> products.updateStatus(id, form.status()), "/admin/products", flash);
    }

    private ProductRequest withImage(ProductRequest form, String imageUrl) {
        return new ProductRequest(form.productName(), form.description(), form.price(), imageUrl, form.status());
    }

    private String form(Model model, Long id) {
        model.addAttribute("id", id);
        model.addAttribute("action", id == null ? "/admin/products" : "/admin/products/" + id);
        model.addAttribute("currentImageUrl", id == null ? null : products.getById(id).imgUrl());
        return "admin/products/form";
    }
}