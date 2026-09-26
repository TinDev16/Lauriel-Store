package com.lauriel.store.controller.web;

import com.lauriel.store.dto.request.BulkProductVariantRequest;
import com.lauriel.store.dto.request.ProductVariantRequest;
import com.lauriel.store.entity.enums.ActiveStatus;
import com.lauriel.store.service.ColorService;
import com.lauriel.store.service.ProductService;
import com.lauriel.store.service.ProductVariantService;
import com.lauriel.store.service.SizeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/products/{productId}/variants")
public class WebVariantController {
    private final ProductService products;
    private final ProductVariantService variants;
    private final SizeService sizes;
    private final ColorService colors;

    public WebVariantController(ProductService products, ProductVariantService variants,
            SizeService sizes, ColorService colors) {
        this.products = products;
        this.variants = variants;
        this.sizes = sizes;
        this.colors = colors;
    }

    @GetMapping
    public String list(@PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size, Model model) {
        model.addAttribute("product", products.getById(productId));
        var result = variants.listAdmin(productId, page, size);
        var groups = result.content().stream().collect(java.util.stream.Collectors.groupingBy(
                com.lauriel.store.dto.response.ProductVariantResponse::color,
                java.util.LinkedHashMap::new, java.util.stream.Collectors.toList()));
        model.addAttribute("page", result);
        model.addAttribute("variantGroups", groups);
        return "admin/products/variants";
    }

    @GetMapping("/new")
    public String createForm(@PathVariable Long productId, Model model) {
        var product = products.getById(productId);
        model.addAttribute("form", new BulkProductVariantRequest(
                List.of(), List.of(), product.price(), ActiveStatus.ACTIVE));
        return bulkForm(productId, model);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long productId, @PathVariable Long id, Model model) {
        var variant = variants.detail(productId, id);
        model.addAttribute("form", new ProductVariantRequest(variant.size().sizeId(),
                variant.color().colorId(), variant.sku(), variant.price(), variant.status()));
        model.addAttribute("sku", variant.sku());
        model.addAttribute("quantity", variant.quantity());
        return editForm(productId, id, model);
    }

    @PostMapping
    public String create(@PathVariable Long productId,
            @Valid @ModelAttribute("form") BulkProductVariantRequest form,
            BindingResult errors, Model model, RedirectAttributes flash) {
        final ProductVariantService.BulkCreateResult[] result = new ProductVariantService.BulkCreateResult[1];
        if (!WebActions.save(() -> result[0] = variants.createBulk(productId, form), errors)) {
            return bulkForm(productId, model);
        }
        String message = "Đã tạo " + result[0].created() + " biến thể.";
        if (result[0].skipped() > 0) {
            message += " Bỏ qua " + result[0].skipped() + " tổ hợp đã tồn tại.";
        }
        flash.addFlashAttribute("successMessage", message);
        return "redirect:/admin/products/" + productId + "/variants";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long productId, @PathVariable Long id,
            @Valid @ModelAttribute("form") ProductVariantRequest form,
            BindingResult errors, Model model, RedirectAttributes flash) {
        var variant = variants.detail(productId, id);
        model.addAttribute("sku", variant.sku());
        model.addAttribute("quantity", variant.quantity());
        if (!WebActions.save(() -> variants.updateForProduct(productId, id, form), errors)) {
            return editForm(productId, id, model);
        }
        return WebActions.saved("/admin/products/" + productId + "/variants", flash);
    }

    private String bulkForm(Long productId, Model model) {
        model.addAttribute("product", products.getById(productId));
        model.addAttribute("sizes", sizes.list());
        model.addAttribute("colors", colors.list());
        model.addAttribute("action", "/admin/products/" + productId + "/variants");
        return "admin/products/variant-bulk-form";
    }

    private String editForm(Long productId, Long id, Model model) {
        model.addAttribute("product", products.getById(productId));
        model.addAttribute("id", id);
        model.addAttribute("sizes", sizes.list());
        model.addAttribute("colors", colors.list());
        model.addAttribute("action", "/admin/products/" + productId + "/variants/" + id);
        return "admin/products/variant-form";
    }
}