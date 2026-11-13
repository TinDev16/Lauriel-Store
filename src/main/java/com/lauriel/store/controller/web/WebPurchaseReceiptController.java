package com.lauriel.store.controller.web;

import com.lauriel.store.dto.request.PurchaseReceiptCreateRequest;
import com.lauriel.store.dto.request.PurchaseReceiptDetailRequest;
import com.lauriel.store.dto.request.PurchaseReceiptItemUpdateRequest;
import com.lauriel.store.dto.response.InventoryResponse;
import com.lauriel.store.dto.response.PurchaseReceiptResponse;
import com.lauriel.store.security.StoreUserDetails;
import com.lauriel.store.service.InventoryService;
import com.lauriel.store.service.PurchaseReceiptService;
import com.lauriel.store.service.SupplierService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/purchase-receipts")
public class WebPurchaseReceiptController {

    private final PurchaseReceiptService receipts;
    private final SupplierService suppliers;
    private final InventoryService inventory;
    private final com.lauriel.store.service.PurchaseReceiptExcelExportService exportService;

    public WebPurchaseReceiptController(
        PurchaseReceiptService receipts,
        SupplierService suppliers,
        InventoryService inventory,
        com.lauriel.store.service.PurchaseReceiptExcelExportService exportService
    ) {
        this.receipts = receipts;
        this.suppliers = suppliers;
        this.inventory = inventory;
        this.exportService = exportService;
    }

    @GetMapping("/export.xlsx")
    public void exportExcel(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) com.lauriel.store.entity.enums.PurchaseReceiptStatus status,
        @RequestParam(required = false) java.time.LocalDateTime from,
        @RequestParam(required = false) java.time.LocalDateTime to,
        jakarta.servlet.http.HttpServletResponse response
    ) throws java.io.IOException {
        String timestamp = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .format(java.time.LocalDateTime.now());
        String filename = "purchase_receipts_" + timestamp + ".xlsx";

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(
            org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + filename + "\""
        );
        exportService.exportReceipts(keyword, status, from, to, response.getOutputStream());
        response.flushBuffer();
    }

    @GetMapping
    public String list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size,
        @RequestParam(required = false) String keyword,
        @RequestParam(
            required = false
        ) com.lauriel.store.entity.enums.PurchaseReceiptStatus status,
        @RequestParam(required = false) java.time.LocalDateTime from,
        @RequestParam(required = false) java.time.LocalDateTime to,
        Model model
    ) {
        var resultPage = receipts.listDetailed(page, size, keyword, status, from, to);
        var pageTotalAmount = resultPage
            .content()
            .stream()
            .map(r -> r.totalAmount() != null ? r.totalAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean isCancelledTab = (status == com.lauriel.store.entity.enums.PurchaseReceiptStatus.CANCELLED);
        long activeCount = receipts.countActive();
        long inactiveCount = receipts.countByStatus(com.lauriel.store.entity.enums.PurchaseReceiptStatus.CANCELLED);

        model.addAttribute("page", resultPage);
        model.addAttribute("pageTotalAmount", pageTotalAmount);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("isCancelledTab", isCancelledTab);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("inactiveCount", inactiveCount);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "admin/purchase-receipts/list";
    }

    @GetMapping("/inactive")
    public String listInactive(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) java.time.LocalDateTime from,
        @RequestParam(required = false) java.time.LocalDateTime to,
        Model model
    ) {
        return list(page, size, keyword, com.lauriel.store.entity.enums.PurchaseReceiptStatus.CANCELLED, from, to, model);
    }

    @GetMapping("/new")
    public String createForm(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size,
        @RequestParam(required = false) Long supplierId,
        Model model
    ) {
        model.addAttribute(
            "form",
            new PurchaseReceiptCreateRequest(supplierId)
        );
        return form(page, size, model);
    }

    @PostMapping
    public String create(
        @Valid @ModelAttribute("form") PurchaseReceiptCreateRequest request,
        BindingResult errors,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size,
        @AuthenticationPrincipal StoreUserDetails actor,
        Model model,
        RedirectAttributes flash
    ) {
        if (errors.hasErrors()) return form(page, size, model);
        var result = new PurchaseReceiptResponse[1];
        if (
            !WebActions.save(
                () -> result[0] = receipts.create(request, actor.getUserId()),
                errors
            )
        ) {
            return form(page, size, model);
        }
        return WebActions.saved(
            "/admin/purchase-receipts/" + result[0].receiptId(),
            flash
        );
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute(
                "form",
                new PurchaseReceiptDetailRequest(null, null, null)
            );
        }
        return detailModel(id, model);
    }

    @GetMapping("/{id}/preview")
    public String preview(@PathVariable Long id, Model model) {
        model.addAttribute("receipt", receipts.detail(id));
        return "admin/purchase-receipts/preview";
    }

    @PostMapping("/{id}/items")
    public String add(
        @PathVariable Long id,
        @Valid @ModelAttribute("form") PurchaseReceiptDetailRequest request,
        BindingResult errors,
        Model model,
        RedirectAttributes flash
    ) {
        if (!WebActions.save(() -> receipts.addItem(id, request), errors)) {
            return detailModel(id, model);
        }
        return WebActions.saved("/admin/purchase-receipts/" + id, flash);
    }

    @PostMapping("/{id}/items/{variantId}")
    public String update(
        @PathVariable Long id,
        @PathVariable Long variantId,
        @Valid @ModelAttribute PurchaseReceiptItemUpdateRequest request,
        BindingResult errors,
        RedirectAttributes flash
    ) {
        if (errors.hasErrors()) {
            flash.addFlashAttribute(
                "errorMessage",
                "Số lượng và đơn giá phải là số dương hợp lệ."
            );
            return "redirect:/admin/purchase-receipts/" + id;
        }
        return WebActions.action(
            () -> receipts.updateItem(id, variantId, request),
            "/admin/purchase-receipts/" + id,
            flash
        );
    }

    @PostMapping("/{id}/items/{variantId}/remove")
    public String remove(
        @PathVariable Long id,
        @PathVariable Long variantId,
        RedirectAttributes flash
    ) {
        return WebActions.action(
            () -> receipts.deleteItem(id, variantId),
            "/admin/purchase-receipts/" + id,
            flash
        );
    }

    @PostMapping("/{id}/complete")
    public String complete(
        @PathVariable Long id,
        @AuthenticationPrincipal StoreUserDetails actor,
        RedirectAttributes flash
    ) {
        return WebActions.action(
            () -> receipts.complete(id, actor.getUserId()),
            "/admin/purchase-receipts/" + id,
            flash
        );
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes flash) {
        return WebActions.action(
            () -> receipts.cancel(id),
            "/admin/purchase-receipts/" + id,
            flash
        );
    }

    private String detailModel(Long id, Model model) {
        model.addAttribute("receipt", receipts.detail(id));
        var variants = inventory.selectableVariants();
        var products = variants
            .stream()
            .collect(
                Collectors.toMap(
                    InventoryResponse::productId,
                    variant -> variant,
                    (current, ignored) -> current,
                    LinkedHashMap::new
                )
            );
        model.addAttribute("receiptProducts", products);
        model.addAttribute("variants", variants);
        return "admin/purchase-receipts/detail";
    }

    private String form(int page, int size, Model model) {
        model.addAttribute("page", suppliers.list(page, size));
        return "admin/purchase-receipts/form";
    }
}
