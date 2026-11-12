package com.lauriel.store.controller.web;

import com.lauriel.store.dto.response.InventoryResponse;
import com.lauriel.store.entity.enums.InventoryTransactionType;
import com.lauriel.store.service.InventoryService;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/inventory")
public class WebInventoryController {

    private final InventoryService inventory;
    private final com.lauriel.store.service.InventoryExcelExportService exportService;

    public WebInventoryController(
        InventoryService inventory,
        com.lauriel.store.service.InventoryExcelExportService exportService
    ) {
        this.inventory = inventory;
        this.exportService = exportService;
    }

    @GetMapping("/export.xlsx")
    public void exportExcel(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Integer threshold,
        @RequestParam(required = false) Long variantId,
        @RequestParam(required = false) InventoryTransactionType type,
        @RequestParam(required = false) LocalDateTime from,
        @RequestParam(required = false) LocalDateTime to,
        jakarta.servlet.http.HttpServletResponse response
    ) throws java.io.IOException {
        String timestamp = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .format(LocalDateTime.now());
        String filename = "inventory_" + timestamp + ".xlsx";

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(
            org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + filename + "\""
        );
        exportService.exportInventory(keyword, threshold, variantId, type, from, to, response.getOutputStream());
        response.flushBuffer();
    }

    @GetMapping
    public String list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Integer threshold,
        @RequestParam(defaultValue = "product") String view,
        Model model
    ) {
        if ("variant".equalsIgnoreCase(view)) {
            int th = threshold != null ? threshold : 5;
            model.addAttribute("page", inventory.list(page, size, keyword, th));
        } else {
            model.addAttribute(
                "page",
                inventory.listProducts(page, size, keyword, threshold)
            );
        }
        model.addAttribute("keyword", keyword);
        model.addAttribute("threshold", threshold);
        model.addAttribute("viewMode", view);
        return "admin/inventory/list";
    }

    @GetMapping("/{productId}")
    public String detail(@PathVariable Long productId, Model model) {
        var detail = inventory.productDetail(productId);
        var groups = detail
            .variants()
            .stream()
            .collect(
                Collectors.groupingBy(
                    InventoryResponse::color,
                    LinkedHashMap::new,
                    Collectors.toList()
                )
            );
        model.addAttribute("detail", detail);
        model.addAttribute("variantGroups", groups);
        return "admin/inventory/detail";
    }

    @GetMapping("/transactions")
    public String history(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long variantId,
        @RequestParam(required = false) InventoryTransactionType type,
        @RequestParam(required = false) LocalDateTime from,
        @RequestParam(required = false) LocalDateTime to,
        Model model
    ) {
        model.addAttribute(
            "page",
            inventory.history(page, size, keyword, variantId, type, from, to)
        );
        model.addAttribute("keyword", keyword);
        model.addAttribute("variantId", variantId);
        var selectableVariants = inventory.selectableVariants();
        var products = selectableVariants
            .stream()
            .collect(
                Collectors.toMap(
                    InventoryResponse::productId,
                    InventoryResponse::productName,
                    (current, ignored) -> current,
                    LinkedHashMap::new
                )
            );
        model.addAttribute("inventoryProducts", products);
        model.addAttribute("variants", selectableVariants);
        model.addAttribute("type", type);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "admin/inventory/transactions";
    }
}
