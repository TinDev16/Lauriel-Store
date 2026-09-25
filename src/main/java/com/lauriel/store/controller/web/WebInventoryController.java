package com.lauriel.store.controller.web;
import com.lauriel.store.service.InventoryService;
import com.lauriel.store.entity.enums.InventoryTransactionType;
import java.time.LocalDateTime;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
@RequestMapping("/admin/inventory")
public class WebInventoryController {
    private final InventoryService inventory;
    public WebInventoryController(InventoryService inventory) { this.inventory=inventory; }
    @GetMapping public String list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="12") int size,
            @RequestParam(required=false) Integer threshold,Model m) {
        m.addAttribute("page",inventory.list(page,size,threshold)); m.addAttribute("threshold",threshold); return "admin/inventory/list";
    }
    @GetMapping("/transactions") public String history(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="12") int size,
            @RequestParam(required=false) Long variantId,@RequestParam(required=false) InventoryTransactionType type,
            @RequestParam(required=false) LocalDateTime from,@RequestParam(required=false) LocalDateTime to,Model m) {
        m.addAttribute("page",inventory.history(page,size,variantId,type,from,to)); m.addAttribute("variantId",variantId);
        m.addAttribute("variants",inventory.selectableVariants());
        m.addAttribute("type",type); m.addAttribute("from",from); m.addAttribute("to",to); return "admin/inventory/transactions";
    }
}
