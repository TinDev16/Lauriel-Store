package com.lauriel.store.service;

import com.lauriel.store.exception.BadRequestException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductImageStorage {
    public static final String PUBLIC_PREFIX = "/uploads/products/";
    private static final long MAX_BYTES = 20L * 1024 * 1024;
    private static final Logger log = LoggerFactory.getLogger(ProductImageStorage.class);
    private final Path directory;

    public ProductImageStorage(@Value("${app.images.directory:uploads/products}") String location) {
        directory = Paths.get(location).toAbsolutePath().normalize();
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tạo thư mục lưu hình ảnh", exception);
        }
    }

    public String store(MultipartFile file) {
        if (file == null) return null;
        if (file.isEmpty()) {
            if (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) return null;
            throw new BadRequestException("Hình ảnh được chọn đang trống.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("Hình ảnh không được vượt quá 20 MB.");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể đọc hình ảnh được chọn", exception);
        }
        if (content.length > MAX_BYTES) {
            throw new BadRequestException("Hình ảnh không được vượt quá 20 MB.");
        }
        String extension = extension(content);
        String filename = UUID.randomUUID() + extension;
        Path target = directory.resolve(filename);
        try {
            Files.write(target, content, StandardOpenOption.CREATE_NEW);
        } catch (IOException exception) {
            try { Files.deleteIfExists(target); } catch (IOException cleanup) { exception.addSuppressed(cleanup); }
            throw new IllegalStateException("Không thể lưu hình ảnh", exception);
        }
        return PUBLIC_PREFIX + filename;
    }

    public void deleteIfManaged(String url) {
        if (url == null || !url.startsWith(PUBLIC_PREFIX)) return;
        String filename = url.substring(PUBLIC_PREFIX.length());
        if (!filename.matches("[0-9a-fA-F-]{36}\\.(png|jpg|gif|webp)")) return;
        try {
            Files.deleteIfExists(directory.resolve(filename));
        } catch (IOException exception) {
            log.warn("Không thể xóa hình ảnh cũ: {}", filename, exception);
        }
    }

    private static String extension(byte[] bytes) {
        if (bytes.length >= 8 && unsigned(bytes[0]) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N'
                && bytes[3] == 'G' && unsigned(bytes[4]) == 13 && unsigned(bytes[5]) == 10
                && unsigned(bytes[6]) == 26 && unsigned(bytes[7]) == 10) return ".png";
        if (bytes.length >= 4 && unsigned(bytes[0]) == 0xff && unsigned(bytes[1]) == 0xd8
                && unsigned(bytes[2]) == 0xff) return ".jpg";
        if (bytes.length >= 6 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F'
                && bytes[3] == '8' && (bytes[4] == '7' || bytes[4] == '9') && bytes[5] == 'a') return ".gif";
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F'
                && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E'
                && bytes[10] == 'B' && bytes[11] == 'P') return ".webp";
        throw new BadRequestException("Chỉ chấp nhận ảnh PNG, JPEG, GIF hoặc WebP hợp lệ.");
    }

    private static int unsigned(byte value) { return value & 0xff; }
}