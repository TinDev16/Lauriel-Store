package com.lauriel.store.controller.api.admin;

import com.lauriel.store.dto.request.ColorRequest;
import com.lauriel.store.dto.response.ColorResponse;
import com.lauriel.store.service.ColorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/colors")
public class AdminColorController {

    private final ColorService service;

    public AdminColorController(ColorService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ColorResponse create(@Valid @RequestBody ColorRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ColorResponse update(
        @PathVariable @Positive Long id,
        @Valid @RequestBody ColorRequest request
    ) {
        return service.update(id, request);
    }
}
