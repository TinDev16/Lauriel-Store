package com.lauriel.store.controller.web;

import com.lauriel.store.dto.request.ProductRequest;
import com.lauriel.store.dto.request.ProductStatusRequest;
import com.lauriel.store.entity.enums.ActiveStatus;
import com.lauriel.store.exception.BadRequestException;
import com.lauriel.store.service.CategoryService;
import com.lauriel.store.service.ProductImageService;
import com.lauriel.store.service.ProductImageStorage;
import com.lauriel.store.service.ProductService;
import com.lauriel.store.service.ColorService;
import com.lauriel.store.service.ProductVariantService;
import jakarta.validation.Valid;
import java.util.Arrays;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/products")
public class WebProductController {
    private final ProductService products;
    private final ProductVariantService variants;
    private final ProductImageService galleries;
    private final ProductImageStorage storage;
    private final CategoryService categories;
    private final ColorService colors;

    public WebProductController(ProductService products, ProductVariantService variants,
            ProductImageService galleries, ProductImageStorage storage, CategoryService categories,
            ColorService colors) {
        this.products = products;
        this.variants = variants;
        this.galleries = galleries;
        this.storage = storage;
        this.categories = categories;
        this.colors = colors;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) ActiveStatus status, Model model) {
        var result = products.listAdmin(page, size, keyword, categoryId, status);
        model.addAttribute("page", result);
        model.addAttribute("counts", variants.counts(result.content().stream().map(p -> p.productId()).toList()));
        model.addAttribute("categories", categories.listAdmin());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("status", status);
        return "admin/products/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new ProductRequest("", null, null, null, null, ActiveStatus.ACTIVE));
        return form(model, null, null);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        var product = products.getById(id);
        model.addAttribute("form", new ProductRequest(product.productName(), product.category().categoryId(),
                product.description(), product.price(), product.imgUrl(), product.status()));
        return form(model, id, product.category().categoryId());
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") ProductRequest request, BindingResult errors,
            @RequestParam(name = "imageFile", required = false) MultipartFile legacyFile,
            @RequestParam(name = "imageFiles", required = false) MultipartFile[] imageFiles,
            @RequestParam(name = "imageColorIds", required = false) Long[] imageColorIds,
            Model model, RedirectAttributes flash) {
        validateImages(legacyFile, imageFiles, errors);
        if (errors.hasErrors()) return form(model, null, request.categoryId());
        var savedRequest = withImage(request, null);
        final com.lauriel.store.dto.response.ProductResponse[] created = new com.lauriel.store.dto.response.ProductResponse[1];
        if (!WebActions.save(() -> created[0] = products.create(savedRequest), errors)) {
            return form(model, null, request.categoryId());
        }
        addImages(created[0].productId(), legacyFile, imageFiles, imageColorIds);
        return WebActions.saved("/admin/products", flash);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") ProductRequest request,
            BindingResult errors,
            @RequestParam(name = "imageFile", required = false) MultipartFile legacyFile,
            @RequestParam(name = "imageFiles", required = false) MultipartFile[] imageFiles,
            @RequestParam(name = "imageColorIds", required = false) Long[] imageColorIds,
            Model model, RedirectAttributes flash) {
        validateImages(legacyFile, imageFiles, errors);
        if (errors.hasErrors()) return form(model, id, request.categoryId());
        String primary = products.getById(id).imgUrl();
        if (!WebActions.save(() -> products.update(id, withImage(request, primary)), errors)) {
            return form(model, id, request.categoryId());
        }
        addImages(id, legacyFile, imageFiles, imageColorIds);
        return WebActions.saved("/admin/products", flash);
    }

    @PostMapping("/{id}/images")
    public String uploadImages(@PathVariable Long id,
            @RequestParam("imageFiles") MultipartFile[] imageFiles,
            @RequestParam(name = "imageColorIds", required = false) Long[] imageColorIds,
            RedirectAttributes flash) {
        return WebActions.action(() -> galleries.add(id, imageFiles, imageColorIds), "/admin/products/" + id + "/edit", flash);
    }

    @PostMapping("/{id}/images/{imageId}/primary")
    public String primary(@PathVariable Long id, @PathVariable Long imageId, RedirectAttributes flash) {
        return WebActions.action(() -> galleries.setPrimary(id, imageId),
                "/admin/products/" + id + "/edit", flash);
    }

    @PostMapping("/{id}/images/{imageId}/delete")
    public String deleteImage(@PathVariable Long id, @PathVariable Long imageId, RedirectAttributes flash) {
        return WebActions.action(() -> galleries.delete(id, imageId),
                "/admin/products/" + id + "/edit", flash);
    }

    @PostMapping("/{id}/status")
    public String status(@PathVariable Long id, @Valid @ModelAttribute ProductStatusRequest request,
            BindingResult errors, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            flash.addFlashAttribute("errorMessage", "Trạng thái không hợp lệ.");
            return "redirect:/admin/products";
        }
        return WebActions.action(() -> products.updateStatus(id, request.status()), "/admin/products", flash);
    }

    private void validateImages(MultipartFile legacy, MultipartFile[] files, BindingResult errors) {
        try {
            if (selected(legacy)) storage.validate(legacy);
            if (files != null) for (MultipartFile file : files) if (selected(file)) storage.validate(file);
            long count = (selected(legacy) ? 1 : 0)
                    + (files == null ? 0 : Arrays.stream(files).filter(this::selected).count());
            if (count > ProductImageService.MAX_IMAGES) {
                throw new BadRequestException("Mỗi sản phẩm chỉ được chọn tối đa 8 hình ảnh.");
            }
        } catch (BadRequestException exception) {
            errors.reject("image", exception.getMessage());
        }
    }

    private void addImages(Long id, MultipartFile legacy, MultipartFile[] files, Long[] imageColorIds) {
        if (selected(legacy)) galleries.replaceLegacyImage(id, legacy);
        if (files != null && Arrays.stream(files).anyMatch(this::selected)) galleries.add(id, files, imageColorIds);
    }

    private boolean selected(MultipartFile file) {
        return file != null && (!file.isEmpty()
                || (file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()));
    }

    private ProductRequest withImage(ProductRequest request, String imageUrl) {
        return new ProductRequest(request.productName(), request.categoryId(), request.description(),
                request.price(), imageUrl, request.status());
    }

    private String form(Model model, Long id, Long currentCategoryId) {
        model.addAttribute("id", id);
        model.addAttribute("action", id == null ? "/admin/products" : "/admin/products/" + id);
        model.addAttribute("categories", categories.listSelectable(currentCategoryId));
        model.addAttribute("productImages", id == null ? java.util.List.of() : galleries.list(id));
        model.addAttribute("colors", colors.list());
        return "admin/products/form";
    }
}
