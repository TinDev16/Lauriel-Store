package com.lauriel.store.controller.web;
import com.lauriel.store.service.SupplierService;
import com.lauriel.store.dto.request.*;
import com.lauriel.store.entity.enums.ActiveStatus;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
@RequestMapping("/admin/suppliers")
public class WebSupplierController {
    private final SupplierService suppliers;
    public WebSupplierController(SupplierService suppliers) { this.suppliers=suppliers; }
    @GetMapping public String list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="12") int size,Model m) {
        m.addAttribute("page",suppliers.list(page,size)); return "admin/suppliers/list";
    }
    @GetMapping("/new") public String createForm(Model m) { m.addAttribute("form",new SupplierRequest("",null,null,null,ActiveStatus.ACTIVE)); return form(m,null); }
    @GetMapping("/{id}/edit") public String edit(@PathVariable Long id,Model m) {
        var s=suppliers.detail(id); m.addAttribute("form",new SupplierRequest(s.supplierName(),s.phone(),s.email(),s.address(),s.status())); return form(m,id);
    }
    @PostMapping public String create(@Valid @ModelAttribute("form") SupplierRequest f,BindingResult e,Model m,RedirectAttributes flash) {
        if(!WebActions.save(() -> suppliers.create(f),e)) return form(m,null); return WebActions.saved("/admin/suppliers",flash);
    }
    @PostMapping("/{id}") public String update(@PathVariable Long id,@Valid @ModelAttribute("form") SupplierRequest f,BindingResult e,Model m,RedirectAttributes flash) {
        if(!WebActions.save(() -> suppliers.update(id,f),e)) return form(m,id); return WebActions.saved("/admin/suppliers",flash);
    }
    @PostMapping("/{id}/status") public String status(@PathVariable Long id,@Valid @ModelAttribute ProductStatusRequest f,BindingResult e,RedirectAttributes flash) {
        if(e.hasErrors()) {flash.addFlashAttribute("errorMessage","Trạng thái không hợp lệ.");return "redirect:/admin/suppliers";}
        return WebActions.action(() -> suppliers.updateStatus(id,f.status()),"/admin/suppliers",flash);
    }
    private String form(Model m,Long id) {m.addAttribute("id",id);m.addAttribute("action","/admin/suppliers"+(id==null?"":"/"+id));return "admin/suppliers/form";}
}
