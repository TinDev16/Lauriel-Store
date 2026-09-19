package com.lauriel.store.controller.web;
import com.lauriel.store.service.PromotionService;
import com.lauriel.store.dto.request.*;
import com.lauriel.store.entity.enums.*;
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
    public WebPromotionController(PromotionService promotions) {this.promotions=promotions;}
    @GetMapping public String list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="12") int size,Model m) {
        m.addAttribute("page",promotions.list(page,size,false));return "admin/promotions/list";
    }
    @GetMapping("/new") public String createForm(Model m) {m.addAttribute("form",new PromotionRequest("",null,null,null,null,ActiveStatus.ACTIVE));return form(m,null);}
    @GetMapping("/{id}/edit") public String edit(@PathVariable Long id,Model m) {
        var v=promotions.detail(id);m.addAttribute("form",new PromotionRequest(v.promotionName(),v.description(),v.discountPercent(),v.startDate(),v.endDate(),v.status()));return form(m,id);
    }
    @PostMapping public String create(@Valid @ModelAttribute("form") PromotionRequest f,BindingResult e,Model m,RedirectAttributes flash) {
        if(!WebActions.save(() -> promotions.create(f),e)) return form(m,null);
        return WebActions.saved("/admin/promotions",flash);
    }
    @PostMapping("/{id}") public String update(@PathVariable Long id,@Valid @ModelAttribute("form") PromotionRequest f,BindingResult e,Model m,RedirectAttributes flash) {
        if(!WebActions.save(() -> promotions.update(id,f),e)) return form(m,id);
        return WebActions.saved("/admin/promotions",flash);
    }
    @PostMapping("/{id}/status") public String status(@PathVariable Long id,@Valid @ModelAttribute ProductStatusRequest f,BindingResult e,RedirectAttributes flash) {
        if(e.hasErrors()){flash.addFlashAttribute("errorMessage","Trạng thái không hợp lệ.");return "redirect:/admin/promotions";}
        return WebActions.action(() -> promotions.updateStatus(id,f.status()),"/admin/promotions",flash);
    }
    private String form(Model m,Long id){m.addAttribute("id",id);m.addAttribute("action","/admin/promotions"+(id==null?"":"/"+id));return "admin/promotions/form";}

    @GetMapping("/{id}/products") public String products(@PathVariable Long id,@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="12") int size,Model m) {
        m.addAttribute("promotion",promotions.detail(id)); m.addAttribute("page",promotions.products(id,page,size)); return "admin/promotions/products";
    }
    @PostMapping("/{id}/products") public String addProduct(@PathVariable Long id,@Valid @ModelAttribute ProductAssociationForm f,BindingResult e,RedirectAttributes flash) {
        if(e.hasErrors()){flash.addFlashAttribute("errorMessage","Mã sản phẩm phải lớn hơn 0.");return "redirect:/admin/promotions/"+id+"/products";}
        return WebActions.action(() -> promotions.addProduct(id,f.productId()),"/admin/promotions/"+id+"/products",flash);
    }
    @PostMapping("/{id}/products/{productId}/remove") public String removeProduct(@PathVariable Long id,@PathVariable Long productId,RedirectAttributes flash) {
        return WebActions.action(() -> promotions.removeProduct(id,productId),"/admin/promotions/"+id+"/products",flash);
    }
    public record ProductAssociationForm(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long productId) {}
}
