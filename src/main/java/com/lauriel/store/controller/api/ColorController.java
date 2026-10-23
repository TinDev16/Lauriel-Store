package com.lauriel.store.controller.api;

import com.lauriel.store.dto.response.ColorResponse;
import com.lauriel.store.service.ColorService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/colors")
public class ColorController {

    private final ColorService service;

    public ColorController(ColorService service) {
        this.service = service;
    }

    @GetMapping
    public List<ColorResponse> list() {
        return service.list();
    }
}
