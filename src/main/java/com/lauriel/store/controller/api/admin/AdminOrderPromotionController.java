package com.lauriel.store.controller.api.admin;

import com.lauriel.store.dto.request.OrderPromotionRequest;
import com.lauriel.store.dto.response.*;
import com.lauriel.store.entity.enums.ActiveStatus;
import com.lauriel.store.service.OrderPromotionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/order-promotions")
public class AdminOrderPromotionController {
    private final OrderPromotionService service;
    public AdminOrderPromotionController(OrderPromotionService service){this.service=service;}
    @GetMapping public PageResponse<OrderPromotionResponse> list(@RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="12") @Min(1) @Max(100) int size,@RequestParam(required=false) String keyword,
            @RequestParam(required=false) ActiveStatus status){return service.list(page,size,keyword,status);}
    @GetMapping("/{id}") public OrderPromotionResponse detail(@PathVariable @Positive Long id){return service.detail(id);}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public OrderPromotionResponse create(@Valid @RequestBody OrderPromotionRequest r){return service.create(r);}
    @PutMapping("/{id}") public OrderPromotionResponse update(@PathVariable @Positive Long id,@Valid @RequestBody OrderPromotionRequest r){return service.update(id,r);}
    @PatchMapping("/{id}/status") public OrderPromotionResponse status(@PathVariable @Positive Long id,@RequestParam ActiveStatus status){return service.updateStatus(id,status);}
}
