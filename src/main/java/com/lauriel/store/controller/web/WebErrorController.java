package com.lauriel.store.controller.web;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
@Controller
public class WebErrorController {
    @GetMapping("/403") @ResponseStatus(HttpStatus.FORBIDDEN)
    public String denied() { return "error/403"; }
}
