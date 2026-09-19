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
@RequestMapping("/admin/products/{productId}/variants")
public class WebVariantController {
    private final ProductService products; private final ProductVariantService variants;
    private final SizeService sizes; private final ColorService colors;
    public WebVariantController(ProductService products,ProductVariantService variants,SizeService sizes,ColorService colors) {
        this.products=products; this.variants=variants; this.sizes=sizes; this.colors=colors;
    }
    @GetMapping public String list(@PathVariable Long productId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="12") int size,Model m) {
        m.addAttribute("product",products.getById(productId)); m.addAttribute("page",variants.listAdmin(productId,page,size)); return "admin/products/variants";
    }
    @GetMapping("/new") public String createForm(@PathVariable Long productId,Model m) {
        m.addAttribute("form",new ProductVariantRequest(null,null,"",null,ActiveStatus.ACTIVE)); return form(productId,null,m);
    }
    @GetMapping("/{id}/edit") public String edit(@PathVariable Long productId,@PathVariable Long id,Model m) {
        var v=variants.detail(productId,id); m.addAttribute("form",new ProductVariantRequest(v.size().sizeId(),v.color().colorId(),v.sku(),v.price(),v.status()));
        m.addAttribute("quantity",v.quantity()); return form(productId,id,m);
    }
    @PostMapping public String create(@PathVariable Long productId,@Valid @ModelAttribute("form") ProductVariantRequest f,BindingResult e,Model m,RedirectAttributes flash) {
        if(!WebActions.save(() -> variants.create(productId,f),e)) return form(productId,null,m);
        return WebActions.saved("/admin/products/"+productId+"/variants",flash);
    }
    @PostMapping("/{id}") public String update(@PathVariable Long productId,@PathVariable Long id,@Valid @ModelAttribute("form") ProductVariantRequest f,BindingResult e,Model m,RedirectAttributes flash) {
        var v=variants.detail(productId,id); m.addAttribute("quantity",v.quantity());
        if(!WebActions.save(() -> variants.updateForProduct(productId,id,f),e)) return form(productId,id,m);
        return WebActions.saved("/admin/products/"+productId+"/variants",flash);
    }
    private String form(Long productId,Long id,Model m) {
        m.addAttribute("product",products.getById(productId)); m.addAttribute("id",id);
        m.addAttribute("sizes",sizes.list()); m.addAttribute("colors",colors.list());
        m.addAttribute("action","/admin/products/"+productId+"/variants"+(id==null?"":"/"+id)); return "admin/products/variant-form";
    }
}
