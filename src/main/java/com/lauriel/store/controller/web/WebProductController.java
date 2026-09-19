package com.lauriel.store.controller.web;
import com.lauriel.store.service.*;
import com.lauriel.store.dto.request.*;
import com.lauriel.store.entity.enums.ActiveStatus;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
@RequestMapping("/admin/products")
public class WebProductController {
    private final ProductService products;
    private final ProductVariantService variants;
    public WebProductController(ProductService products,ProductVariantService variants) { this.products=products; this.variants=variants; }
    @GetMapping public String list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="12") int size,
            @RequestParam(required=false) String keyword,@RequestParam(required=false) ActiveStatus status,Model m) {
        var result=products.listAdmin(page,size,keyword,status); m.addAttribute("page",result);
        m.addAttribute("counts",variants.counts(result.content().stream().map(p -> p.productId()).toList()));
        m.addAttribute("keyword",keyword); m.addAttribute("status",status); return "admin/products/list";
    }
    @GetMapping("/new") public String createForm(Model m) {
        m.addAttribute("form",new ProductRequest("",null,null,null,ActiveStatus.ACTIVE)); return form(m,null);
    }
    @GetMapping("/{id}/edit") public String edit(@PathVariable Long id,Model m) {
        var p=products.getById(id); m.addAttribute("form",new ProductRequest(p.productName(),p.description(),p.price(),p.imgUrl(),p.status()));
        return form(m,id);
    }
    @PostMapping public String create(@Valid @ModelAttribute("form") ProductRequest form,BindingResult errors,Model m,RedirectAttributes flash) {
        if (!WebActions.save(() -> products.create(form),errors)) return form(m,null);
        return WebActions.saved("/admin/products",flash);
    }
    @PostMapping("/{id}") public String update(@PathVariable Long id,@Valid @ModelAttribute("form") ProductRequest form,BindingResult errors,Model m,RedirectAttributes flash) {
        if (!WebActions.save(() -> products.update(id,form),errors)) return form(m,id);
        return WebActions.saved("/admin/products",flash);
    }
    @PostMapping("/{id}/status") public String status(@PathVariable Long id,@Valid @ModelAttribute ProductStatusRequest form,BindingResult errors,RedirectAttributes flash) {
        if(errors.hasErrors()) { flash.addFlashAttribute("errorMessage","Trạng thái không hợp lệ."); return "redirect:/admin/products"; }
        return WebActions.action(() -> products.updateStatus(id,form.status()),"/admin/products",flash);
    }
    private String form(Model m,Long id) { m.addAttribute("id",id); m.addAttribute("action",id==null?"/admin/products":"/admin/products/"+id); return "admin/products/form"; }
}
