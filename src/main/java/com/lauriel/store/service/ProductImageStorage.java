package com.lauriel.store.service;

import com.lauriel.store.exception.BadRequestException;
import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductImageStorage {

    public static final String PUBLIC_PREFIX = "/uploads/products/";
    public static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Logger log = LoggerFactory.getLogger(
        ProductImageStorage.class
    );
    private final Path directory;

    public ProductImageStorage(
        @Value("${app.images.directory:uploads/products}") String location
    ) {
        directory = Paths.get(location).toAbsolutePath().normalize();
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new IllegalStateException(
                "Không thể tạo thư mục lưu hình ảnh",
                e
            );
        }
    }

    public void validate(MultipartFile file) {
        readValidated(file);
    }

    public String store(Long productId, MultipartFile file) {
        if (productId == null || productId <= 0) throw new BadRequestException(
            "Sản phẩm không hợp lệ."
        );
        byte[] content = readValidated(file);
        String extension = extension(content);
        Path productDirectory = directory
            .resolve(productId.toString())
            .normalize();
        if (
            !productDirectory.startsWith(directory)
        ) throw new BadRequestException("Đường dẫn hình ảnh không hợp lệ.");
        String filename = UUID.randomUUID() + extension;
        Path target = productDirectory.resolve(filename).normalize();
        if (!target.startsWith(productDirectory)) throw new BadRequestException(
            "Đường dẫn hình ảnh không hợp lệ."
        );
        try {
            Files.createDirectories(productDirectory);
            Files.write(target, content, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException cleanup) {
                e.addSuppressed(cleanup);
            }
            throw new IllegalStateException("Không thể lưu hình ảnh", e);
        }
        return PUBLIC_PREFIX + productId + "/" + filename;
    }

    public void deleteIfManaged(String url) {
        if (url == null || !url.startsWith(PUBLIC_PREFIX)) return;
        String relative = url.substring(PUBLIC_PREFIX.length());
        if (
            !relative.matches("[0-9]+/[0-9a-fA-F-]{36}\\.(png|jpg|webp)")
        ) return;
        Path target = directory.resolve(relative).normalize();
        if (!target.startsWith(directory)) return;
        try {
            Files.deleteIfExists(target);
            Path parent = target.getParent();
            if (parent != null && !parent.equals(directory)) {
                try (var stream = Files.list(parent)) {
                    if (stream.findAny().isEmpty()) Files.deleteIfExists(
                        parent
                    );
                }
            }
        } catch (IOException e) {
            log.warn("Không thể xóa hình ảnh: {}", relative, e);
        }
    }

    private static byte[] readValidated(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException(
            "Hình ảnh được chọn đang trống."
        );
        if (file.getSize() > MAX_BYTES) throw new BadRequestException(
            "Mỗi hình ảnh không được vượt quá 5 MB."
        );
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException(
                "Không thể đọc hình ảnh được chọn",
                e
            );
        }
        if (content.length == 0 || content.length > MAX_BYTES) {
            throw new BadRequestException("Hình ảnh trống hoặc vượt quá 5 MB.");
        }
        String extension = extension(content);
        String mime =
            file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        if (!mime.isBlank() && !allowedMime(mime, extension)) {
            throw new BadRequestException(
                "MIME của hình ảnh không khớp nội dung tệp."
            );
        }
        return content;
    }

    private static boolean allowedMime(String mime, String extension) {
        return (
            (extension.equals(".png") && mime.equals("image/png")) ||
            (extension.equals(".jpg") &&
                (mime.equals("image/jpeg") || mime.equals("image/jpg"))) ||
            (extension.equals(".webp") && mime.equals("image/webp"))
        );
    }

    private static String extension(byte[] b) {
        if (
            b.length >= 8 &&
            u(b[0]) == 0x89 &&
            b[1] == 'P' &&
            b[2] == 'N' &&
            b[3] == 'G' &&
            u(b[4]) == 13 &&
            u(b[5]) == 10 &&
            u(b[6]) == 26 &&
            u(b[7]) == 10
        ) return ".png";
        if (
            b.length >= 4 &&
            u(b[0]) == 0xff &&
            u(b[1]) == 0xd8 &&
            u(b[2]) == 0xff
        ) return ".jpg";
        if (
            b.length >= 12 &&
            b[0] == 'R' &&
            b[1] == 'I' &&
            b[2] == 'F' &&
            b[3] == 'F' &&
            b[8] == 'W' &&
            b[9] == 'E' &&
            b[10] == 'B' &&
            b[11] == 'P'
        ) return ".webp";
        throw new BadRequestException(
            "Chỉ chấp nhận ảnh PNG, JPEG hoặc WebP hợp lệ."
        );
    }

    private static int u(byte value) {
        return value & 0xff;
    }
}
