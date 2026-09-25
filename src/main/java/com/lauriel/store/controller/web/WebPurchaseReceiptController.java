package com.lauriel.store.controller.web;
import com.lauriel.store.service.*;
import com.lauriel.store.dto.request.*;
import com.lauriel.store.security.StoreUserDetails;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
@RequestMapping("/admin/purchase-receipts")
public class WebPurchaseReceiptController {
    private final PurchaseReceiptService receipts; private final SupplierService suppliers; private final InventoryService inventory;
    public WebPurchaseReceiptController(PurchaseReceiptService receipts,SupplierService suppliers,InventoryService inventory) {
        this.receipts=receipts;this.suppliers=suppliers;this.inventory=inventory;
    }
    @GetMapping public String list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="12") int size,Model m) {
        m.addAttribute("page",receipts.list(page,size)); return "admin/purchase-receipts/list";
    }
    @GetMapping("/new") public String createForm(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="12") int size,
            @RequestParam(required=false) Long supplierId,Model m) {
        m.addAttribute("form",new PurchaseReceiptCreateRequest(supplierId)); return form(page,size,m);
    }
    @PostMapping public String create(@Valid @ModelAttribute("form") PurchaseReceiptCreateRequest f,BindingResult e,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="12") int size,
            @AuthenticationPrincipal StoreUserDetails actor,Model m,RedirectAttributes flash) {
        if(e.hasErrors()) return form(page,size,m);
        var result=new com.lauriel.store.dto.response.PurchaseReceiptResponse[1];
        if(!WebActions.save(() -> result[0]=receipts.create(f,actor.getUserId()),e)) return form(page,size,m);
        return WebActions.saved("/admin/purchase-receipts/"+result[0].receiptId(),flash);
    }
    @GetMapping("/{id}") public String detail(@PathVariable Long id,Model m) {
        if(!m.containsAttribute("form")) m.addAttribute("form",new PurchaseReceiptDetailRequest(null,null,null));
        return detailModel(id,m);
    }
    @PostMapping("/{id}/items") public String add(@PathVariable Long id,@Valid @ModelAttribute("form") PurchaseReceiptDetailRequest f,
            BindingResult e,Model m,RedirectAttributes flash) {
        if(!WebActions.save(() -> receipts.addItem(id,f),e)) return detailModel(id,m);
        return WebActions.saved("/admin/purchase-receipts/"+id,flash);
    }
    @PostMapping("/{id}/items/{variantId}") public String update(@PathVariable Long id,@PathVariable Long variantId,
            @Valid @ModelAttribute PurchaseReceiptItemUpdateRequest f,BindingResult e,RedirectAttributes flash) {
        if(e.hasErrors()){flash.addFlashAttribute("errorMessage","Số lượng và đơn giá phải là số dương hợp lệ.");return "redirect:/admin/purchase-receipts/"+id;}
        return WebActions.action(() -> receipts.updateItem(id,variantId,f),"/admin/purchase-receipts/"+id,flash);
    }
    @PostMapping("/{id}/items/{variantId}/remove") public String remove(@PathVariable Long id,@PathVariable Long variantId,RedirectAttributes flash) {
        return WebActions.action(() -> receipts.deleteItem(id,variantId),"/admin/purchase-receipts/"+id,flash);
    }
    @PostMapping("/{id}/complete") public String complete(@PathVariable Long id,@AuthenticationPrincipal StoreUserDetails actor,RedirectAttributes flash) {
        return WebActions.action(() -> receipts.complete(id,actor.getUserId()),"/admin/purchase-receipts/"+id,flash);
    }
    @PostMapping("/{id}/cancel") public String cancel(@PathVariable Long id,RedirectAttributes flash) {
        return WebActions.action(() -> receipts.cancel(id),"/admin/purchase-receipts/"+id,flash);
    }
    private String detailModel(Long id,Model m) {
        m.addAttribute("receipt",receipts.detail(id));
        m.addAttribute("variants",inventory.selectableVariants());
        return "admin/purchase-receipts/detail";
    }
    private String form(int page,int size,Model m) {m.addAttribute("page",suppliers.list(page,size));return "admin/purchase-receipts/form";}
}
