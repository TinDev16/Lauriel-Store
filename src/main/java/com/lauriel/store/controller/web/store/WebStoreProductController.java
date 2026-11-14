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

    public WebStoreProductController(
        ProductService products,
        CategoryService categories,
        SizeService sizes,
        ColorService colors
    ) {
        this.products = products;
        this.categories = categories;
        this.sizes = sizes;
        this.colors = colors;
    }

    @GetMapping
    public String list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) Long sizeId,
        @RequestParam(required = false) Long colorId,
        @RequestParam(defaultValue = "createdAt") String sort,
        Model model
    ) {
        String sortBy = switch (sort) {
            case "price", "productName", "createdAt" -> sort;
            default -> "createdAt";
        };
        String direction = "productName".equals(sortBy) ? "asc" : "desc";
        model.addAttribute(
            "page",
            products.listPublic(
                page,
                Math.min(Math.max(size, 1), 24),
                keyword,
                categoryId,
                sizeId,
                colorId,
                sortBy,
                direction
            )
        );
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
    public String detail(
        @PathVariable Long id,
        @CookieValue(name = "lauriel_viewed", required = false) String viewedCookie,
        jakarta.servlet.http.HttpServletResponse response,
        Model model
    ) {
        var product = products.getPublicDetail(id);
        updateRecentlyViewedCookie(id, viewedCookie, response);
        var uniqueSizes = new LinkedHashMap<
            Long,
            com.lauriel.store.dto.response.SizeResponse
        >();
        var uniqueColors = new LinkedHashMap<
            Long,
            com.lauriel.store.dto.response.ColorResponse
        >();
        product.variants().forEach(variant -> {
            uniqueSizes.putIfAbsent(variant.size().sizeId(), variant.size());
            uniqueColors.putIfAbsent(
                variant.color().colorId(),
                variant.color()
            );
        });
        model.addAttribute("product", product);
        model.addAttribute("sizes", uniqueSizes.values());
        model.addAttribute("colors", uniqueColors.values());
        model.addAttribute("related", products.related(id, 6));
        return "store/product-detail";
    }

    @PostMapping("/clear-recently-viewed")
    @ResponseBody
    public java.util.Map<String, Object> clearRecentlyViewed(jakarta.servlet.http.HttpServletResponse response) {
        org.springframework.http.ResponseCookie cookie = org.springframework.http.ResponseCookie.from("lauriel_viewed", "")
            .path("/")
            .maxAge(0)
            .sameSite("Lax")
            .httpOnly(false)
            .build();
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString());
        return java.util.Map.of("success", true);
    }

    private void updateRecentlyViewedCookie(
        Long currentId,
        String viewedCookie,
        jakarta.servlet.http.HttpServletResponse response
    ) {
        java.util.List<Long> ids = new java.util.ArrayList<>();
        ids.add(currentId);
        if (viewedCookie != null && !viewedCookie.isBlank()) {
            for (String part : viewedCookie.split("[-_,]")) {
                try {
                    Long pid = Long.valueOf(part.trim());
                    if (!pid.equals(currentId) && !ids.contains(pid)) {
                        ids.add(pid);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        if (ids.size() > 12) {
            ids = ids.subList(0, 12);
        }
        String newVal = ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining("-"));
        org.springframework.http.ResponseCookie cookie = org.springframework.http.ResponseCookie.from("lauriel_viewed", newVal)
            .path("/")
            .maxAge(java.time.Duration.ofDays(30))
            .sameSite("Lax")
            .httpOnly(false)
            .build();
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
