package com.lauriel.store.controller.web;

import com.lauriel.store.dto.request.*;
import com.lauriel.store.entity.enums.*;
import com.lauriel.store.service.CategoryService;
import com.lauriel.store.service.MarketingOverviewService;
import com.lauriel.store.service.OrderPromotionService;
import com.lauriel.store.service.PromotionService;
import com.lauriel.store.service.VoucherService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/promotions")
public class WebPromotionController {

    private final PromotionService promotions;
    private final CategoryService categories;
    private final MarketingOverviewService marketingOverview;
    private final VoucherService vouchers;
    private final OrderPromotionService orderPromotions;

    public WebPromotionController(
        PromotionService promotions,
        CategoryService categories,
        MarketingOverviewService marketingOverview,
        VoucherService vouchers,
        OrderPromotionService orderPromotions
    ) {
        this.promotions = promotions;
        this.categories = categories;
        this.marketingOverview = marketingOverview;
        this.vouchers = vouchers;
        this.orderPromotions = orderPromotions;
    }

    @GetMapping("/overview")
    public String overview(
        @RequestParam(defaultValue = "overview") String tab,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) ActiveStatus status,
        @RequestParam(required = false) DiscountType discountType,
        @RequestParam(required = false) String timeFilter,
        Model m
    ) {
        var overviewData = marketingOverview.getOverview();
        m.addAttribute("overview", overviewData);
        m.addAttribute("currentTab", tab);
        m.addAttribute("statuses", ActiveStatus.values());
        m.addAttribute("discountTypes", DiscountType.values());
        m.addAttribute("keyword", keyword);
        m.addAttribute("status", status);
        m.addAttribute("discountType", discountType);
        m.addAttribute("timeFilter", timeFilter);

        if ("promotions".equalsIgnoreCase(tab)) {
            m.addAttribute(
                "page",
                promotions.list(page, size, false, keyword, status, timeFilter)
            );
        } else if ("vouchers".equalsIgnoreCase(tab)) {
            m.addAttribute(
                "page",
                vouchers.list(page, size, keyword, status, discountType, timeFilter)
            );
        } else if ("order-promotions".equalsIgnoreCase(tab)) {
            m.addAttribute(
                "page",
                orderPromotions.list(page, size, keyword, status, timeFilter)
            );
        }
        return "admin/promotions/overview";
    }

    @GetMapping
    public String list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) ActiveStatus status
    ) {
        String query = "?tab=promotions";
        if (page > 0) query += "&page=" + page;
        if (size != 12) query += "&size=" + size;
        if (keyword != null && !keyword.isBlank()) {
            query += "&keyword=" + java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8);
        }
        if (status != null) query += "&status=" + status.name();
        return "redirect:/admin/promotions/overview" + query;
    }

    @GetMapping("/new")
    public String createForm(Model m) {
        m.addAttribute(
            "form",
            new PromotionRequest(
                "",
                null,
                null,
                null,
                null,
                ActiveStatus.ACTIVE
            )
        );
        return form(m, null);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model m) {
        var v = promotions.detail(id);
        m.addAttribute(
            "form",
            new PromotionRequest(
                v.promotionName(),
                v.description(),
                v.discountPercent(),
                v.startDate(),
                v.endDate(),
                v.status()
            )
        );
        return form(m, id);
    }

    @PostMapping
    public String create(
        @Valid @ModelAttribute("form") PromotionRequest f,
        BindingResult e,
        Model m,
        RedirectAttributes flash
    ) {
        if (!WebActions.save(() -> promotions.create(f), e)) return form(
            m,
            null
        );
        return WebActions.saved("/admin/promotions/overview?tab=promotions", flash);
    }

    @PostMapping("/{id}")
    public String update(
        @PathVariable Long id,
        @Valid @ModelAttribute("form") PromotionRequest f,
        BindingResult e,
        Model m,
        RedirectAttributes flash
    ) {
        if (!WebActions.save(() -> promotions.update(id, f), e)) return form(
            m,
            id
        );
        return WebActions.saved("/admin/promotions/overview?tab=promotions", flash);
    }

    @PostMapping("/{id}/status")
    public String status(
        @PathVariable Long id,
        @Valid @ModelAttribute ProductStatusRequest f,
        BindingResult e,
        RedirectAttributes flash
    ) {
        if (e.hasErrors()) {
            flash.addFlashAttribute("errorMessage", "Trạng thái không hợp lệ.");
            return "redirect:/admin/promotions/overview?tab=promotions";
        }
        return WebActions.action(
            () -> promotions.updateStatus(id, f.status()),
            "/admin/promotions/overview?tab=promotions",
            flash
        );
    }

    private String form(Model m, Long id) {
        m.addAttribute("id", id);
        m.addAttribute(
            "action",
            "/admin/promotions" + (id == null ? "" : "/" + id)
        );
        return "admin/promotions/form";
    }

    @GetMapping("/{id}/products")
    public String products(
        @PathVariable Long id,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) ActiveStatus status,
        @RequestParam(required = false) String candidateKeyword,
        @RequestParam(required = false) Long categoryId,
        Model m
    ) {
        var promotion = promotions.detail(id);
        var now = java.time.LocalDateTime.now();
        boolean enabled = promotion.status() == ActiveStatus.ACTIVE;
        m.addAttribute("promotion", promotion);
        m.addAttribute(
            "promotionActive",
            enabled &&
                !promotion.startDate().isAfter(now) &&
                !promotion.endDate().isBefore(now)
        );
        m.addAttribute("promotionExpired", promotion.endDate().isBefore(now));
        m.addAttribute(
            "promotionPending",
            enabled && promotion.startDate().isAfter(now)
        );
        m.addAttribute(
            "page",
            promotions.products(id, page, size, keyword, status)
        );
        m.addAttribute(
            "selectableProducts",
            promotions.selectableProducts(id, candidateKeyword, categoryId)
        );
        m.addAttribute("categories", categories.listAdmin());
        m.addAttribute("keyword", keyword);
        m.addAttribute("status", status);
        m.addAttribute("candidateKeyword", candidateKeyword);
        m.addAttribute("categoryId", categoryId);
        return "admin/promotions/products";
    }

    @PostMapping("/{id}/products")
    public String addProduct(
        @PathVariable Long id,
        @RequestParam(required = false) Long productId,
        @RequestParam(required = false) java.util.List<Long> productIds,
        @RequestParam(required = false) Long categoryId,
        RedirectAttributes flash
    ) {
        var selected = new java.util.ArrayList<Long>();
        if (productIds != null) selected.addAll(productIds);
        if (productId != null) selected.add(productId);
        String redirect =
            "/admin/promotions/" +
            id +
            "/products" +
            (categoryId == null ? "" : "?categoryId=" + categoryId);
        return WebActions.action(
            () -> promotions.addProducts(id, selected),
            redirect,
            flash
        );
    }

    @GetMapping("/{id}/products/{productId}/variants")
    public String variantScope(
        @PathVariable Long id,
        @PathVariable Long productId,
        Model model
    ) {
        model.addAttribute("scope", promotions.variantScope(id, productId));
        return "admin/promotions/variant-scope";
    }

    @PostMapping("/{id}/products/{productId}/variants")
    public String saveVariantScope(
        @PathVariable Long id,
        @PathVariable Long productId,
        @RequestParam(defaultValue = "ALL") String scope,
        @RequestParam(required = false) java.util.List<Long> variantIds,
        RedirectAttributes flash
    ) {
        return WebActions.action(
            () ->
                promotions.configureVariantScope(
                    id,
                    productId,
                    "ALL".equalsIgnoreCase(scope),
                    variantIds
                ),
            "/admin/promotions/" + id + "/products/" + productId + "/variants",
            flash
        );
    }

    @PostMapping("/{id}/products/{productId}/remove")
    public String removeProduct(
        @PathVariable Long id,
        @PathVariable Long productId,
        RedirectAttributes flash
    ) {
        return WebActions.action(
            () -> promotions.removeProduct(id, productId),
            "/admin/promotions/" + id + "/products",
            flash
        );
    }
}
