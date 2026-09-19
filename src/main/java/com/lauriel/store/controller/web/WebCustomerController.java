package com.lauriel.store.controller.web;
import com.lauriel.store.service.CustomerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
public class WebCustomerController {
    private final CustomerService customers;
    public WebCustomerController(CustomerService customers) { this.customers=customers; }
    @GetMapping("/admin/customers") public String list(@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="12") int size,Model m) { m.addAttribute("page",customers.list(page,size)); return "admin/customers/list"; }
}
