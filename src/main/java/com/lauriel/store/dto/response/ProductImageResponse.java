package com.lauriel.store.dto.response;

public record ProductImageResponse(Long id, String url, boolean primary, int order,
                                   Long sizeId, String sizeName) {
    public ProductImageResponse(Long id, String url, boolean primary, int order) {
        this(id, url, primary, order, null, null);
    }
}
