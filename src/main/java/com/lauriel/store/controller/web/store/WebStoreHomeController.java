package com.lauriel.store.controller.web.store;

import com.lauriel.store.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebStoreHomeController {

    private final ProductService products;
    private final CategoryService categories;
    private final PromotionService promotions;
    private final OrderPromotionService orderPromotions;
    private final VoucherService vouchers;

    public WebStoreHomeController(
        ProductService products,
        CategoryService categories,
        PromotionService promotions,
        OrderPromotionService orderPromotions,
        VoucherService vouchers
    ) {
        this.products = products;
        this.categories = categories;
        this.promotions = promotions;
        this.orderPromotions = orderPromotions;
        this.vouchers = vouchers;
    }

    @GetMapping("/shop")
    public String home(Model model) {
        model.addAttribute("categories", categories.listActive());
        model.addAttribute("products", products.featured(8));
        model.addAttribute("promotions", promotions.list(0, 6, true).content());
        model.addAttribute(
            "orderPromotions",
            orderPromotions.active(0, 6).content()
        );
        model.addAttribute("vouchers", vouchers.listAvailable(0, 6).content());
        return "store/home";
    }
}
