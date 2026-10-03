package com.lauriel.store.controller.web;
import com.lauriel.store.service.VoucherService;
import com.lauriel.store.dto.request.*;
import com.lauriel.store.entity.enums.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
@RequestMapping("/admin/vouchers")
public class WebVoucherController {
    private final VoucherService vouchers;
    public WebVoucherController(VoucherService vouchers) {this.vouchers=vouchers;}
    @GetMapping public String list(@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="12") int size,
            @RequestParam(required=false) String keyword,
            @RequestParam(required=false) ActiveStatus status,
            @RequestParam(required=false) DiscountType discountType, Model m) {
        m.addAttribute("page",vouchers.list(page,size,keyword,status,discountType));
        m.addAttribute("keyword",keyword); m.addAttribute("status",status);
        m.addAttribute("discountType",discountType); return "admin/vouchers/list";
    }
    @GetMapping("/new") public String createForm(Model m) {m.addAttribute("form",new VoucherRequest("","",DiscountType.PERCENT,null,java.math.BigDecimal.ZERO,null,0,null,null,ActiveStatus.ACTIVE));return form(m,null);}
    @GetMapping("/{id}/edit") public String edit(@PathVariable Long id,Model m) {
        var v=vouchers.detail(id);m.addAttribute("form",new VoucherRequest(v.code(),v.voucherName(),v.discountType(),v.discountValue(),v.minOrderAmount(),v.maxDiscountAmount(),v.quantity(),v.startDate(),v.endDate(),v.status()));return form(m,id);
    }
    @PostMapping public String create(@Valid @ModelAttribute("form") VoucherRequest f,BindingResult e,Model m,RedirectAttributes flash) {
        if(!WebActions.save(() -> vouchers.create(f),e)) return form(m,null);
        return WebActions.saved("/admin/vouchers",flash);
    }
    @PostMapping("/{id}") public String update(@PathVariable Long id,@Valid @ModelAttribute("form") VoucherRequest f,BindingResult e,Model m,RedirectAttributes flash) {
        if(!WebActions.save(() -> vouchers.update(id,f),e)) return form(m,id);
        return WebActions.saved("/admin/vouchers",flash);
    }
    @PostMapping("/{id}/status") public String status(@PathVariable Long id,@Valid @ModelAttribute ProductStatusRequest f,BindingResult e,RedirectAttributes flash) {
        if(e.hasErrors()){flash.addFlashAttribute("errorMessage","Trạng thái không hợp lệ.");return "redirect:/admin/vouchers";}
        return WebActions.action(() -> vouchers.updateStatus(id,f.status()),"/admin/vouchers",flash);
    }
    private String form(Model m,Long id){m.addAttribute("id",id);m.addAttribute("action","/admin/vouchers"+(id==null?"":"/"+id));return "admin/vouchers/form";}
}
