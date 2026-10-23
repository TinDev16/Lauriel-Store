package com.lauriel.store.controller.api;

import com.lauriel.store.dto.response.SizeResponse;
import com.lauriel.store.service.SizeService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sizes")
public class SizeController {

    private final SizeService service;

    public SizeController(SizeService service) {
        this.service = service;
    }

    @GetMapping
    public List<SizeResponse> list() {
        return service.list();
    }
}
