package com.lauriel.store.controller.web;
import com.lauriel.store.service.ColorService;
import com.lauriel.store.dto.request.ColorRequest;
import com.lauriel.store.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
@RequestMapping("/admin/colors")
public class WebColorController {
    private final ColorService service;
    public WebColorController(ColorService service) { this.service=service; }
    @GetMapping public String list(Model m) { m.addAttribute("items",service.list()); return "admin/colors/list"; }
    @GetMapping("/new") public String createForm(Model m) {
        m.addAttribute("form",new ColorRequest("")); return form(m,null);
    }
    @GetMapping("/{id}/edit") public String edit(@PathVariable Long id,Model m) {
        var item=service.detail(id);
        m.addAttribute("form",new ColorRequest(item.colorName())); return form(m,id);
    }
    @PostMapping public String create(@Valid @ModelAttribute("form") ColorRequest f,BindingResult e,Model m,RedirectAttributes flash) {
        if(!WebActions.save(() -> service.create(f),e)) return form(m,null);
        return WebActions.saved("/admin/colors",flash);
    }
    @PostMapping("/{id}") public String update(@PathVariable Long id,@Valid @ModelAttribute("form") ColorRequest f,BindingResult e,Model m,RedirectAttributes flash) {
        if(!WebActions.save(() -> service.update(id,f),e)) return form(m,id);
        return WebActions.saved("/admin/colors",flash);
    }
    private String form(Model m,Long id) { m.addAttribute("action","/admin/colors"+(id==null?"":"/"+id)); m.addAttribute("id",id); return "admin/colors/form"; }
}
