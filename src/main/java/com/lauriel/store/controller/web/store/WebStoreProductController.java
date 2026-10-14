package com.lauriel.store.controller.web.store;

import com.lauriel.store.service.*;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/shop/products")
public class WebStoreProductController {
    private final ProductService products;
    private final CategoryService categories;
    private final SizeService sizes;
    private final ColorService colors;

    public WebStoreProductController(ProductService products, CategoryService categories,
            SizeService sizes, ColorService colors) {
        this.products = products; this.categories = categories; this.sizes = sizes; this.colors = colors;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size, @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId, @RequestParam(required = false) Long sizeId,
            @RequestParam(required = false) Long colorId, @RequestParam(defaultValue = "createdAt") String sort,
            Model model) {
        String sortBy = switch (sort) {
            case "price", "productName", "createdAt" -> sort;
            default -> "createdAt";
        };
        String direction = "productName".equals(sortBy) ? "asc" : "desc";
        model.addAttribute("page", products.listPublic(page, Math.min(Math.max(size, 1), 24), keyword,
                categoryId, sizeId, colorId, sortBy, direction));
        model.addAttribute("categories", categories.listActive());
        model.addAttribute("sizes", sizes.list());
        model.addAttribute("colors", colors.list());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("sizeId", sizeId);
        model.addAttribute("colorId", colorId);
        model.addAttribute("sort", sortBy);
        return "store/products";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        var product = products.getPublicDetail(id);
        var uniqueSizes = new LinkedHashMap<Long, com.lauriel.store.dto.response.SizeResponse>();
        var uniqueColors = new LinkedHashMap<Long, com.lauriel.store.dto.response.ColorResponse>();
        product.variants().forEach(variant -> {
            uniqueSizes.putIfAbsent(variant.size().sizeId(), variant.size());
            uniqueColors.putIfAbsent(variant.color().colorId(), variant.color());
        });
        model.addAttribute("product", product);
        model.addAttribute("sizes", uniqueSizes.values());
        model.addAttribute("colors", uniqueColors.values());
        model.addAttribute("related", products.related(id, 6));
        return "store/product-detail";
    }
}
