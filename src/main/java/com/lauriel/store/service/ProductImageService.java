package com.lauriel.store.service;

import com.lauriel.store.dto.response.ProductImageResponse;
import com.lauriel.store.entity.Color;
import com.lauriel.store.entity.Product;
import com.lauriel.store.entity.ProductImage;
import com.lauriel.store.exception.BadRequestException;
import com.lauriel.store.exception.ResourceNotFoundException;
import com.lauriel.store.repository.ColorRepository;
import com.lauriel.store.repository.ProductImageRepository;
import com.lauriel.store.repository.ProductRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductImageService {

    public static final int MAX_IMAGES = 8;
    private final ProductRepository products;
    private final ProductImageRepository images;
    private final ProductImageStorage storage;
    private final ColorRepository colors;

    public ProductImageService(
        ProductRepository products,
        ProductImageRepository images,
        ProductImageStorage storage,
        ColorRepository colors
    ) {
        this.products = products;
        this.images = images;
        this.storage = storage;
        this.colors = colors;
    }

    @Transactional(readOnly = true)
    public List<ProductImageResponse> list(Long productId) {
        return images
            .findByProduct_ProductIdOrderByDisplayOrderAscProductImageIdAsc(
                productId
            )
            .stream()
            .map(ProductImageService::response)
            .toList();
    }

    @Transactional
    public List<ProductImageResponse> add(
        Long productId,
        MultipartFile[] files
    ) {
        return add(productId, files, null);
    }

    @Transactional
    public List<ProductImageResponse> add(
        Long productId,
        MultipartFile[] files,
        Long[] colorIds
    ) {
        Product product = lockProduct(productId);
        List<MultipartFile> selected = new ArrayList<>();
        List<Long> selectedColorIds = new ArrayList<>();
        if (files != null) {
            for (int index = 0; index < files.length; index++) {
                MultipartFile file = files[index];
                if (file == null || file.isEmpty()) continue;
                selected.add(file);
                selectedColorIds.add(
                    colorIds != null && index < colorIds.length
                        ? colorIds[index]
                        : null
                );
            }
        }
        if (selected.isEmpty()) return list(productId);
        var existing =
            images.findByProduct_ProductIdOrderByDisplayOrderAscProductImageIdAsc(
                productId
            );
        if (existing.size() + selected.size() > MAX_IMAGES) {
            throw new BadRequestException(
                "Mỗi sản phẩm chỉ được có tối đa 8 hình ảnh."
            );
        }
        Map<Long, Color> colorById = resolveColors(selectedColorIds);
        int order =
            existing
                .stream()
                .mapToInt(ProductImage::getDisplayOrder)
                .max()
                .orElse(-1) + 1;
        boolean needsPrimary = existing
            .stream()
            .noneMatch(ProductImage::isPrimary);
        List<String> stored = new ArrayList<>();
        try {
            for (int index = 0; index < selected.size(); index++) {
                String url = storage.store(productId, selected.get(index));
                stored.add(url);
                ProductImage image = new ProductImage();
                image.setProduct(product);
                Long selectedColorId = selectedColorIds.get(index);
                image.setColor(
                    selectedColorId == null
                        ? null
                        : colorById.get(selectedColorId)
                );
                image.setImageUrl(url);
                image.setDisplayOrder(order++);
                image.setPrimary(needsPrimary);
                if (needsPrimary) {
                    product.setImgUrl(url);
                    needsPrimary = false;
                }
                images.save(image);
            }
            products.save(product);
            images.flush();
            return list(productId);
        } catch (RuntimeException exception) {
            stored.forEach(storage::deleteIfManaged);
            throw exception;
        }
    }

    @Transactional
    public List<ProductImageResponse> replaceLegacyImage(
        Long productId,
        MultipartFile file
    ) {
        if (file == null || file.isEmpty()) return list(productId);
        Product product = lockProduct(productId);
        var old =
            images.findByProduct_ProductIdOrderByDisplayOrderAscProductImageIdAsc(
                productId
            );
        String url = storage.store(productId, file);
        try {
            images.deleteAll(old);
            images.flush();
            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setImageUrl(url);
            image.setPrimary(true);
            image.setDisplayOrder(0);
            images.saveAndFlush(image);
            product.setImgUrl(url);
            products.saveAndFlush(product);
            old.forEach(i -> storage.deleteIfManaged(i.getImageUrl()));
            return List.of(response(image));
        } catch (RuntimeException exception) {
            storage.deleteIfManaged(url);
            throw exception;
        }
    }

    @Transactional
    public List<ProductImageResponse> setPrimary(Long productId, Long imageId) {
        Product product = lockProduct(productId);
        var all =
            images.findByProduct_ProductIdOrderByDisplayOrderAscProductImageIdAsc(
                productId
            );
        ProductImage selected = all
            .stream()
            .filter(i -> i.getProductImageId().equals(imageId))
            .findFirst()
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Không tìm thấy hình ảnh sản phẩm"
                )
            );
        for (var image : all) image.setPrimary(image == selected);
        images.saveAll(all);
        product.setImgUrl(selected.getImageUrl());
        products.save(product);
        return all.stream().map(ProductImageService::response).toList();
    }

    @Transactional
    public List<ProductImageResponse> delete(Long productId, Long imageId) {
        Product product = lockProduct(productId);
        var all = new ArrayList<>(
            images.findByProduct_ProductIdOrderByDisplayOrderAscProductImageIdAsc(
                productId
            )
        );
        ProductImage removed = all
            .stream()
            .filter(i -> i.getProductImageId().equals(imageId))
            .findFirst()
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Không tìm thấy hình ảnh sản phẩm"
                )
            );
        boolean wasPrimary = removed.isPrimary();
        images.delete(removed);
        all.remove(removed);
        if (wasPrimary) {
            if (all.isEmpty()) product.setImgUrl(null);
            else {
                ProductImage next = all.get(0);
                next.setPrimary(true);
                images.save(next);
                product.setImgUrl(next.getImageUrl());
            }
            products.save(product);
        }
        images.flush();
        storage.deleteIfManaged(removed.getImageUrl());
        return all.stream().map(ProductImageService::response).toList();
    }

    private Map<Long, Color> resolveColors(List<Long> requested) {
        Set<Long> ids = requested
            .stream()
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        Map<Long, Color> result = colors
            .findAllById(ids)
            .stream()
            .collect(Collectors.toMap(Color::getColorId, Function.identity()));
        if (result.size() != ids.size()) throw new BadRequestException(
            "Màu sắc gắn với hình ảnh không hợp lệ."
        );
        return result;
    }

    private Product lockProduct(Long id) {
        return products
            .findByIdForUpdate(id)
            .orElseThrow(() ->
                new ResourceNotFoundException("Không tìm thấy sản phẩm")
            );
    }

    private static ProductImageResponse response(ProductImage image) {
        return new ProductImageResponse(
            image.getProductImageId(),
            image.getImageUrl(),
            image.isPrimary(),
            image.getDisplayOrder(),
            image.getColor() == null ? null : image.getColor().getColorId(),
            image.getColor() == null ? null : image.getColor().getColorName()
        );
    }
}
