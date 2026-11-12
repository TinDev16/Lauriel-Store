package com.lauriel.store.service;

import com.lauriel.store.dto.excel.ImportRowError;
import com.lauriel.store.dto.excel.ProductImportResult;
import com.lauriel.store.dto.request.ProductRequest;
import com.lauriel.store.dto.request.ProductVariantRequest;
import com.lauriel.store.dto.response.ProductResponse;
import com.lauriel.store.entity.Category;
import com.lauriel.store.entity.Color;
import com.lauriel.store.entity.Size;
import com.lauriel.store.entity.enums.ActiveStatus;
import com.lauriel.store.repository.CategoryRepository;
import com.lauriel.store.repository.ColorRepository;
import com.lauriel.store.repository.ProductVariantRepository;
import com.lauriel.store.repository.SizeRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductExcelImportService {

    public static final int MAX_PRODUCTS = 1000;
    public static final int MAX_VARIANTS = 5000;
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final ProductService productService;
    private final ProductVariantService productVariantService;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final SizeRepository sizeRepository;
    private final ColorRepository colorRepository;

    public ProductExcelImportService(
        ProductService productService,
        ProductVariantService productVariantService,
        ProductVariantRepository productVariantRepository,
        CategoryRepository categoryRepository,
        SizeRepository sizeRepository,
        ColorRepository colorRepository
    ) {
        this.productService = productService;
        this.productVariantService = productVariantService;
        this.productVariantRepository = productVariantRepository;
        this.categoryRepository = categoryRepository;
        this.sizeRepository = sizeRepository;
        this.colorRepository = colorRepository;
    }

    public void generateTemplate(OutputStream outputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Styles styles = createStyles(workbook);

            // Sheet 1: Products
            Sheet prodSheet = workbook.createSheet("Products");
            prodSheet.setDisplayGridlines(true);
            Row prodHeader = prodSheet.createRow(0);
            prodHeader.setHeightInPoints(26);
            String[] prodHeaders = {
                "product_key",
                "product_name",
                "category",
                "description",
                "base_price",
                "status"
            };
            for (int i = 0; i < prodHeaders.length; i++) {
                Cell cell = prodHeader.createCell(i);
                cell.setCellValue(prodHeaders[i]);
                cell.setCellStyle(styles.header);
            }

            // Fetch actual reference data if available for friendly samples
            List<Category> allCategories = categoryRepository.findAll();
            List<Size> allSizes = sizeRepository.findAll();
            List<Color> allColors = colorRepository.findAll();

            String sampleCat1 = allCategories.stream()
                .filter(c -> c.getStatus() == ActiveStatus.ACTIVE)
                .map(Category::getCategoryName)
                .findFirst()
                .orElse("Áo sơ mi");
            String sampleCat2 = allCategories.stream()
                .filter(c -> c.getStatus() == ActiveStatus.ACTIVE)
                .skip(1)
                .map(Category::getCategoryName)
                .findFirst()
                .orElse(sampleCat1);

            // Sample rows for Products
            Object[][] prodSamples = {
                { "SP01", "Áo sơ mi Oxford Lauriel", sampleCat1, "Chất liệu cotton cao cấp, thoáng mát", 350000.0, "ACTIVE" },
                { "SP02", "Quần tây âu Slimfit", sampleCat2, "Form dáng chuẩn, co giãn nhẹ", 450000.0, "ACTIVE" }
            };
            for (int r = 0; r < prodSamples.length; r++) {
                Row row = prodSheet.createRow(r + 1);
                row.setHeightInPoints(20);
                row.createCell(0).setCellValue((String) prodSamples[r][0]);
                row.createCell(1).setCellValue((String) prodSamples[r][1]);
                row.createCell(2).setCellValue((String) prodSamples[r][2]);
                row.createCell(3).setCellValue((String) prodSamples[r][3]);
                Cell priceCell = row.createCell(4);
                priceCell.setCellValue((Double) prodSamples[r][4]);
                priceCell.setCellStyle(styles.currency);
                row.createCell(5).setCellValue((String) prodSamples[r][5]);
            }
            prodSheet.createFreezePane(0, 1);
            prodSheet.setAutoFilter(new CellRangeAddress(0, prodSamples.length, 0, prodHeaders.length - 1));
            for (int i = 0; i < prodHeaders.length; i++) {
                prodSheet.autoSizeColumn(i);
                prodSheet.setColumnWidth(i, Math.max(prodSheet.getColumnWidth(i) + 1200, 4000));
            }

            // Sheet 2: Variants
            Sheet varSheet = workbook.createSheet("Variants");
            varSheet.setDisplayGridlines(true);
            Row varHeader = varSheet.createRow(0);
            varHeader.setHeightInPoints(26);
            String[] varHeaders = {
                "product_key",
                "sku",
                "size",
                "color",
                "variant_price",
                "status"
            };
            for (int i = 0; i < varHeaders.length; i++) {
                Cell cell = varHeader.createCell(i);
                cell.setCellValue(varHeaders[i]);
                cell.setCellStyle(styles.header);
            }

            String s1 = allSizes.size() > 0 ? allSizes.get(0).getSizeName() : "S";
            String s2 = allSizes.size() > 1 ? allSizes.get(1).getSizeName() : "M";
            String s3 = allSizes.size() > 2 ? allSizes.get(2).getSizeName() : "L";
            String c1 = allColors.size() > 0 ? allColors.get(0).getColorName() : "Trắng";
            String c2 = allColors.size() > 1 ? allColors.get(1).getColorName() : "Đen";

            // Sample rows for Variants
            Object[][] varSamples = {
                { "SP01", "SP01-" + s1 + "-" + c1, s1, c1, 350000.0, "ACTIVE" },
                { "SP01", "SP01-" + s2 + "-" + c1, s2, c1, 350000.0, "ACTIVE" },
                { "SP01", "SP01-" + s3 + "-" + c2, s3, c2, 350000.0, "ACTIVE" },
                { "SP02", "SP02-" + s1 + "-" + c2, s1, c2, 450000.0, "ACTIVE" },
                { "SP02", "SP02-" + s2 + "-" + c2, s2, c2, 450000.0, "ACTIVE" }
            };
            for (int r = 0; r < varSamples.length; r++) {
                Row row = varSheet.createRow(r + 1);
                row.setHeightInPoints(20);
                row.createCell(0).setCellValue((String) varSamples[r][0]);
                row.createCell(1).setCellValue((String) varSamples[r][1]);
                row.createCell(2).setCellValue((String) varSamples[r][2]);
                row.createCell(3).setCellValue((String) varSamples[r][3]);
                Cell priceCell = row.createCell(4);
                priceCell.setCellValue((Double) varSamples[r][4]);
                priceCell.setCellStyle(styles.currency);
                row.createCell(5).setCellValue((String) varSamples[r][5]);
            }
            varSheet.createFreezePane(0, 1);
            varSheet.setAutoFilter(new CellRangeAddress(0, varSamples.length, 0, varHeaders.length - 1));
            for (int i = 0; i < varHeaders.length; i++) {
                varSheet.autoSizeColumn(i);
                varSheet.setColumnWidth(i, Math.max(varSheet.getColumnWidth(i) + 1200, 4000));
            }

            // Sheet 3: References
            Sheet refSheet = workbook.createSheet("Huong_Dan_Tham_Chieu");
            refSheet.setDisplayGridlines(true);

            Row refTitle = refSheet.createRow(0);
            Cell refTitleCell = refTitle.createCell(0);
            refTitleCell.setCellValue("BẢNG THAM CHIẾU DANH MỤC, KÍCH CỠ, MÀU SẮC ĐANG CÓ");
            refTitleCell.setCellStyle(styles.title);

            Row noteRow = refSheet.createRow(1);
            noteRow.createCell(0).setCellValue(
                "Lưu ý: Điền trực tiếp Tên Danh Mục vào cột 'category' ở sheet Products, Tên Kích Cỡ vào cột 'size' và Tên Màu Sắc vào cột 'color' ở sheet Variants. product_key dùng để liên kết sản phẩm và biến thể trong cùng file."
            );

            // Headers for Reference Table
            Row refHeaderRow = refSheet.createRow(3);
            refHeaderRow.createCell(0).setCellValue("Tên Danh Mục Hợp Lệ");
            refHeaderRow.createCell(1).setCellValue("ID (Tham khảo)");
            refHeaderRow.createCell(3).setCellValue("Tên Kích Cỡ Hợp Lệ");
            refHeaderRow.createCell(4).setCellValue("ID (Tham khảo)");
            refHeaderRow.createCell(6).setCellValue("Tên Màu Sắc Hợp Lệ");
            refHeaderRow.createCell(7).setCellValue("ID (Tham khảo)");
            for (int col : new int[]{0, 1, 3, 4, 6, 7}) {
                refHeaderRow.getCell(col).setCellStyle(styles.subHeader);
            }

            int maxRefRows = Math.max(allCategories.size(), Math.max(allSizes.size(), allColors.size()));

            for (int r = 0; r < maxRefRows; r++) {
                Row row = refSheet.createRow(r + 4);
                if (r < allCategories.size()) {
                    Category cat = allCategories.get(r);
                    row.createCell(0).setCellValue(cat.getCategoryName() + (cat.getStatus() != ActiveStatus.ACTIVE ? " (Ngừng hoạt động)" : ""));
                    row.createCell(1).setCellValue(cat.getCategoryId());
                }
                if (r < allSizes.size()) {
                    Size s = allSizes.get(r);
                    row.createCell(3).setCellValue(s.getSizeName());
                    row.createCell(4).setCellValue(s.getSizeId());
                }
                if (r < allColors.size()) {
                    Color c = allColors.get(r);
                    row.createCell(6).setCellValue(c.getColorName());
                    row.createCell(7).setCellValue(c.getColorId());
                }
            }
            for (int i = 0; i <= 8; i++) {
                refSheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
        }
    }

    public ProductImportResult importProducts(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ProductImportResult.failure(
                0, 0, 0, 0,
                List.of(new ImportRowError("General", 0, "file", "", "Vui lòng chọn file Excel để tải lên.")),
                null
            );
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            return ProductImportResult.failure(
                0, 0, 0, 0,
                List.of(new ImportRowError("General", 0, "file", filename != null ? filename : "", "Chỉ chấp nhận file định dạng .xlsx.")),
                null
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return ProductImportResult.failure(
                0, 0, 0, 0,
                List.of(new ImportRowError("General", 0, "file", file.getSize() + " bytes", "Kích thước file vượt quá giới hạn 10MB.")),
                null
            );
        }

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet prodSheet = workbook.getSheet("Products");
            Sheet varSheet = workbook.getSheet("Variants");

            List<ImportRowError> errors = new ArrayList<>();

            if (prodSheet == null) {
                errors.add(new ImportRowError("General", 0, "sheet", "", "File Excel thiếu sheet 'Products'."));
            }
            if (varSheet == null) {
                errors.add(new ImportRowError("General", 0, "sheet", "", "File Excel thiếu sheet 'Variants'."));
            }
            if (!errors.isEmpty()) {
                return ProductImportResult.failure(0, 0, 0, 0, errors, buildErrorReportExcel(errors));
            }

            // Map Headers
            Map<String, Integer> prodHeaderMap = mapHeaders(prodSheet.getRow(0));
            Map<String, Integer> varHeaderMap = mapHeaders(varSheet.getRow(0));

            validateRequiredHeaders("Products", prodHeaderMap, List.of("product_key", "product_name", "description", "base_price", "status"), errors);
            if (!prodHeaderMap.containsKey("category") && !prodHeaderMap.containsKey("category_id")) {
                errors.add(new ImportRowError("Products", 1, "category", "", "Sheet 'Products' thiếu cột bắt buộc: 'category' (hoặc 'category_id')."));
            }

            validateRequiredHeaders("Variants", varHeaderMap, List.of("product_key", "sku", "variant_price", "status"), errors);
            if (!varHeaderMap.containsKey("size") && !varHeaderMap.containsKey("size_id")) {
                errors.add(new ImportRowError("Variants", 1, "size", "", "Sheet 'Variants' thiếu cột bắt buộc: 'size' (hoặc 'size_id')."));
            }
            if (!varHeaderMap.containsKey("color") && !varHeaderMap.containsKey("color_id")) {
                errors.add(new ImportRowError("Variants", 1, "color", "", "Sheet 'Variants' thiếu cột bắt buộc: 'color' (hoặc 'color_id')."));
            }

            if (!errors.isEmpty()) {
                return ProductImportResult.failure(0, 0, 0, 0, errors, buildErrorReportExcel(errors));
            }

            Integer catCol = prodHeaderMap.containsKey("category") ? prodHeaderMap.get("category") : prodHeaderMap.get("category_id");
            Integer sizeCol = varHeaderMap.containsKey("size") ? varHeaderMap.get("size") : varHeaderMap.get("size_id");
            Integer colorCol = varHeaderMap.containsKey("color") ? varHeaderMap.get("color") : varHeaderMap.get("color_id");

            // Pre-load reference maps for validation performance (by ID and by Name)
            Map<Long, Category> categoryById = new HashMap<>();
            Map<String, Category> categoryByName = new HashMap<>();
            categoryRepository.findAll().forEach(c -> {
                categoryById.put(c.getCategoryId(), c);
                if (c.getCategoryName() != null && !c.getCategoryName().isBlank()) {
                    categoryByName.put(c.getCategoryName().trim().toLowerCase(), c);
                }
            });

            Map<Long, Size> sizeById = new HashMap<>();
            Map<String, Size> sizeByName = new HashMap<>();
            sizeRepository.findAll().forEach(s -> {
                sizeById.put(s.getSizeId(), s);
                if (s.getSizeName() != null && !s.getSizeName().isBlank()) {
                    sizeByName.put(s.getSizeName().trim().toLowerCase(), s);
                }
            });

            Map<Long, Color> colorById = new HashMap<>();
            Map<String, Color> colorByName = new HashMap<>();
            colorRepository.findAll().forEach(c -> {
                colorById.put(c.getColorId(), c);
                if (c.getColorName() != null && !c.getColorName().isBlank()) {
                    colorByName.put(c.getColorName().trim().toLowerCase(), c);
                }
            });

            // Parse & Validate Products Sheet
            List<ParsedProduct> parsedProducts = new ArrayList<>();
            Set<String> seenProductKeys = new HashSet<>();
            int totalProdRows = 0;
            int invalidProdRows = 0;

            for (int r = 1; r <= prodSheet.getLastRowNum(); r++) {
                Row row = prodSheet.getRow(r);
                if (row == null || isRowEmpty(row, prodHeaderMap.size())) continue;

                totalProdRows++;
                if (totalProdRows > MAX_PRODUCTS) {
                    errors.add(new ImportRowError("Products", r + 1, "row_limit", "", "Số lượng sản phẩm vượt quá giới hạn tối đa " + MAX_PRODUCTS + " dòng."));
                    break;
                }

                int prevErrorCount = errors.size();
                int rowNum = r + 1;

                String productKey = getCellString(row, prodHeaderMap.get("product_key"));
                if (productKey.isBlank()) {
                    errors.add(new ImportRowError("Products", rowNum, "product_key", productKey, "product_key không được để trống."));
                } else if (productKey.length() > 50) {
                    errors.add(new ImportRowError("Products", rowNum, "product_key", productKey, "product_key vượt quá 50 ký tự."));
                } else if (!seenProductKeys.add(productKey)) {
                    errors.add(new ImportRowError("Products", rowNum, "product_key", productKey, "product_key '" + productKey + "' bị trùng lặp trong file."));
                }

                String productName = getCellString(row, prodHeaderMap.get("product_name"));
                if (productName.isBlank()) {
                    errors.add(new ImportRowError("Products", rowNum, "product_name", productName, "product_name không được để trống."));
                } else if (productName.length() > 200) {
                    errors.add(new ImportRowError("Products", rowNum, "product_name", productName, "product_name vượt quá 200 ký tự."));
                }

                String catStr = getCellString(row, catCol);
                Long categoryId = null;
                if (catStr.isBlank()) {
                    errors.add(new ImportRowError("Products", rowNum, "category", catStr, "category không được để trống."));
                } else {
                    Category cat = categoryByName.get(catStr.trim().toLowerCase());
                    if (cat == null) {
                        try {
                            Long parsedId = Long.parseLong(catStr.replace(".0", "").trim());
                            cat = categoryById.get(parsedId);
                        } catch (NumberFormatException ignored) {}
                    }

                    if (cat == null) {
                        String clean = catStr.replace(".0", "").trim();
                        boolean isNum = !clean.isEmpty() && clean.chars().allMatch(Character::isDigit);
                        String msg = isNum ? "Danh mục #" + clean + " không tồn tại." : "Danh mục '" + catStr + "' không tồn tại trong hệ thống.";
                        errors.add(new ImportRowError("Products", rowNum, "category", catStr, msg));
                    } else if (cat.getStatus() != ActiveStatus.ACTIVE) {
                        errors.add(new ImportRowError("Products", rowNum, "category", catStr, "Danh mục '" + cat.getCategoryName() + "' đang ngừng hoạt động."));
                    } else {
                        categoryId = cat.getCategoryId();
                    }
                }

                String description = getCellString(row, prodHeaderMap.get("description"));
                if (description != null && description.getBytes(StandardCharsets.UTF_8).length > 65535) {
                    errors.add(new ImportRowError("Products", rowNum, "description", "[TEXT]", "Mô tả vượt quá giới hạn lưu trữ cho phép."));
                }

                String priceStr = getCellString(row, prodHeaderMap.get("base_price"));
                BigDecimal basePrice = null;
                if (priceStr.isBlank()) {
                    errors.add(new ImportRowError("Products", rowNum, "base_price", priceStr, "base_price không được để trống."));
                } else {
                    try {
                        basePrice = new BigDecimal(priceStr.replace(",", "").trim());
                        if (basePrice.compareTo(BigDecimal.ZERO) < 0) {
                            errors.add(new ImportRowError("Products", rowNum, "base_price", priceStr, "base_price không được âm."));
                        } else if (basePrice.scale() > 2) {
                            errors.add(new ImportRowError("Products", rowNum, "base_price", priceStr, "base_price tối đa 2 chữ số thập phân."));
                        }
                    } catch (Exception e) {
                        errors.add(new ImportRowError("Products", rowNum, "base_price", priceStr, "base_price phải là số tiền hợp lệ."));
                    }
                }

                String statusStr = getCellString(row, prodHeaderMap.get("status"));
                ActiveStatus status = null;
                if (statusStr.isBlank()) {
                    errors.add(new ImportRowError("Products", rowNum, "status", statusStr, "status không được để trống."));
                } else {
                    try {
                        status = ActiveStatus.valueOf(statusStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        errors.add(new ImportRowError("Products", rowNum, "status", statusStr, "status phải là 'ACTIVE' hoặc 'INACTIVE'."));
                    }
                }

                if (errors.size() > prevErrorCount) {
                    invalidProdRows++;
                } else {
                    parsedProducts.add(new ParsedProduct(productKey, productName, categoryId, description, basePrice, status));
                }
            }

            if (totalProdRows == 0) {
                errors.add(new ImportRowError("Products", 1, "sheet", "", "Sheet 'Products' không có dữ liệu nào để nhập."));
            }

            // Parse & Validate Variants Sheet
            List<ParsedVariant> parsedVariants = new ArrayList<>();
            Set<String> seenSkus = new HashSet<>();
            Set<String> seenVariantCombinations = new HashSet<>();
            int totalVarRows = 0;
            int invalidVarRows = 0;

            for (int r = 1; r <= varSheet.getLastRowNum(); r++) {
                Row row = varSheet.getRow(r);
                if (row == null || isRowEmpty(row, varHeaderMap.size())) continue;

                totalVarRows++;
                if (totalVarRows > MAX_VARIANTS) {
                    errors.add(new ImportRowError("Variants", r + 1, "row_limit", "", "Số lượng biến thể vượt quá giới hạn tối đa " + MAX_VARIANTS + " dòng."));
                    break;
                }

                int prevErrorCount = errors.size();
                int rowNum = r + 1;

                String prodKey = getCellString(row, varHeaderMap.get("product_key"));
                if (prodKey.isBlank()) {
                    errors.add(new ImportRowError("Variants", rowNum, "product_key", prodKey, "product_key không được để trống."));
                } else if (!seenProductKeys.contains(prodKey)) {
                    errors.add(new ImportRowError("Variants", rowNum, "product_key", prodKey, "product_key '" + prodKey + "' không tồn tại trong sheet Products."));
                }

                String sku = getCellString(row, varHeaderMap.get("sku"));
                if (sku.isBlank()) {
                    errors.add(new ImportRowError("Variants", rowNum, "sku", sku, "sku không được để trống."));
                } else if (sku.length() > 100) {
                    errors.add(new ImportRowError("Variants", rowNum, "sku", sku, "sku vượt quá 100 ký tự."));
                } else if (!seenSkus.add(sku.toLowerCase())) {
                    errors.add(new ImportRowError("Variants", rowNum, "sku", sku, "SKU '" + sku + "' bị trùng lặp trong file."));
                } else if (productVariantRepository.existsBySkuIgnoreCase(sku)) {
                    errors.add(new ImportRowError("Variants", rowNum, "sku", sku, "SKU '" + sku + "' đã tồn tại trong hệ thống."));
                }

                String sizeStr = getCellString(row, sizeCol);
                Long sizeId = null;
                if (sizeStr.isBlank()) {
                    errors.add(new ImportRowError("Variants", rowNum, "size", sizeStr, "size không được để trống."));
                } else {
                    Size s = sizeByName.get(sizeStr.trim().toLowerCase());
                    if (s == null) {
                        try {
                            Long parsedId = Long.parseLong(sizeStr.replace(".0", "").trim());
                            s = sizeById.get(parsedId);
                        } catch (NumberFormatException ignored) {}
                    }
                    if (s == null) {
                        String clean = sizeStr.replace(".0", "").trim();
                        boolean isNum = !clean.isEmpty() && clean.chars().allMatch(Character::isDigit);
                        String msg = isNum ? "Kích cỡ #" + clean + " không tồn tại." : "Kích cỡ '" + sizeStr + "' không tồn tại trong hệ thống.";
                        errors.add(new ImportRowError("Variants", rowNum, "size", sizeStr, msg));
                    } else {
                        sizeId = s.getSizeId();
                    }
                }

                String colorStr = getCellString(row, colorCol);
                Long colorId = null;
                if (colorStr.isBlank()) {
                    errors.add(new ImportRowError("Variants", rowNum, "color", colorStr, "color không được để trống."));
                } else {
                    Color c = colorByName.get(colorStr.trim().toLowerCase());
                    if (c == null) {
                        try {
                            Long parsedId = Long.parseLong(colorStr.replace(".0", "").trim());
                            c = colorById.get(parsedId);
                        } catch (NumberFormatException ignored) {}
                    }
                    if (c == null) {
                        String clean = colorStr.replace(".0", "").trim();
                        boolean isNum = !clean.isEmpty() && clean.chars().allMatch(Character::isDigit);
                        String msg = isNum ? "Màu sắc #" + clean + " không tồn tại." : "Màu sắc '" + colorStr + "' không tồn tại trong hệ thống.";
                        errors.add(new ImportRowError("Variants", rowNum, "color", colorStr, msg));
                    } else {
                        colorId = c.getColorId();
                    }
                }

                if (!prodKey.isBlank() && sizeId != null && colorId != null) {
                    String comboKey = prodKey + ":" + sizeId + ":" + colorId;
                    if (!seenVariantCombinations.add(comboKey)) {
                        String sName = sizeById.containsKey(sizeId) ? sizeById.get(sizeId).getSizeName() : String.valueOf(sizeId);
                        String cName = colorById.containsKey(colorId) ? colorById.get(colorId).getColorName() : String.valueOf(colorId);
                        errors.add(new ImportRowError("Variants", rowNum, "combination", "size=" + sName + ", color=" + cName, "Tổ hợp kích cỡ '" + sName + "' và màu sắc '" + cName + "' bị trùng lặp cho cùng một sản phẩm (" + prodKey + ")."));
                    }
                }

                String varPriceStr = getCellString(row, varHeaderMap.get("variant_price"));
                BigDecimal varPrice = null;
                if (varPriceStr.isBlank()) {
                    errors.add(new ImportRowError("Variants", rowNum, "variant_price", varPriceStr, "variant_price không được để trống."));
                } else {
                    try {
                        varPrice = new BigDecimal(varPriceStr.replace(",", "").trim());
                        if (varPrice.compareTo(BigDecimal.ZERO) < 0) {
                            errors.add(new ImportRowError("Variants", rowNum, "variant_price", varPriceStr, "variant_price không được âm."));
                        } else if (varPrice.scale() > 2) {
                            errors.add(new ImportRowError("Variants", rowNum, "variant_price", varPriceStr, "variant_price tối đa 2 chữ số thập phân."));
                        }
                    } catch (Exception e) {
                        errors.add(new ImportRowError("Variants", rowNum, "variant_price", varPriceStr, "variant_price phải là số tiền hợp lệ."));
                    }
                }

                String vStatusStr = getCellString(row, varHeaderMap.get("status"));
                ActiveStatus vStatus = null;
                if (vStatusStr.isBlank()) {
                    errors.add(new ImportRowError("Variants", rowNum, "status", vStatusStr, "status không được để trống."));
                } else {
                    try {
                        vStatus = ActiveStatus.valueOf(vStatusStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        errors.add(new ImportRowError("Variants", rowNum, "status", vStatusStr, "status phải là 'ACTIVE' hoặc 'INACTIVE'."));
                    }
                }

                if (errors.size() > prevErrorCount) {
                    invalidVarRows++;
                } else {
                    parsedVariants.add(new ParsedVariant(prodKey, sku, sizeId, colorId, varPrice, vStatus));
                }
            }

            int validRows = (totalProdRows - invalidProdRows) + (totalVarRows - invalidVarRows);
            int invalidRows = invalidProdRows + invalidVarRows;

            // If ANY error exists, do not write anything to database
            if (!errors.isEmpty()) {
                return ProductImportResult.failure(
                    totalProdRows,
                    totalVarRows,
                    validRows,
                    invalidRows,
                    errors,
                    buildErrorReportExcel(errors)
                );
            }

            // Phase 2: Execute transactional insertion
            return executeImport(parsedProducts, parsedVariants);

        } catch (Exception e) {
            List<ImportRowError> errs = List.of(
                new ImportRowError("General", 0, "system", "", "Lỗi phân tích file Excel: " + e.getMessage())
            );
            return ProductImportResult.failure(0, 0, 0, 0, errs, buildErrorReportExcel(errs));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    protected ProductImportResult executeImport(List<ParsedProduct> products, List<ParsedVariant> variants) {
        Map<String, Long> productKeyToId = new LinkedHashMap<>();

        // 1. Create Products
        for (ParsedProduct p : products) {
            ProductRequest req = new ProductRequest(
                p.productName(),
                p.categoryId(),
                p.description(),
                p.basePrice(),
                null,
                p.status()
            );
            ProductResponse created = productService.create(req);
            productKeyToId.put(p.productKey(), created.productId());
        }

        // 2. Create Variants
        for (ParsedVariant v : variants) {
            Long productId = productKeyToId.get(v.productKey());
            ProductVariantRequest req = new ProductVariantRequest(
                v.sizeId(),
                v.colorId(),
                v.sku(),
                v.variantPrice(),
                v.status()
            );
            productVariantService.create(productId, req);
        }

        return ProductImportResult.success(products.size(), variants.size());
    }

    public byte[] buildErrorReportExcel(List<ImportRowError> errors) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Styles styles = createStyles(workbook);

            Sheet sheet = workbook.createSheet("Chi_Tiet_Loi");
            sheet.setDisplayGridlines(true);

            // Row 0: Title
            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(28);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BÁO CÁO CHI TIẾT LỖI NHẬP FILE EXCEL SẢN PHẨM");
            titleCell.setCellStyle(styles.title);

            // Row 1: Subtitle
            Row subRow = sheet.createRow(1);
            subRow.setHeightInPoints(18);
            subRow.createCell(0).setCellValue(
                "Thời gian tạo báo cáo: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) +
                " | Tổng số lỗi: " + (errors != null ? errors.size() : 0) +
                " | Lưu ý: Không có dữ liệu nào được ghi vào hệ thống. Vui lòng sửa lại các dòng lỗi và nhập lại."
            );

            // Row 3: Headers
            String[] headers = { "STT", "Sheet Bị Lỗi", "Dòng Excel", "Cột Dữ Liệu", "Giá Trị Nhập Vào", "Nguyên Nhân Lỗi Cụ Thể" };
            Row headerRow = sheet.createRow(3);
            headerRow.setHeightInPoints(26);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(styles.errorHeader);
            }

            int r = 4;
            if (errors != null) {
                int stt = 1;
                for (ImportRowError err : errors) {
                    Row row = sheet.createRow(r++);
                    row.setHeightInPoints(20);

                    Cell c0 = row.createCell(0);
                    c0.setCellValue(stt++);
                    c0.setCellStyle(styles.dataCenter);

                    Cell c1 = row.createCell(1);
                    c1.setCellValue(err.sheet());
                    c1.setCellStyle(styles.dataCenter);

                    Cell c2 = row.createCell(2);
                    c2.setCellValue(err.rowNumber() > 0 ? "Dòng " + err.rowNumber() : "—");
                    c2.setCellStyle(styles.dataCenter);

                    Cell c3 = row.createCell(3);
                    c3.setCellValue(err.column());
                    c3.setCellStyle(styles.dataLeft);

                    Cell c4 = row.createCell(4);
                    c4.setCellValue(err.value() != null ? err.value() : "");
                    c4.setCellStyle(styles.dataLeft);

                    Cell c5 = row.createCell(5);
                    c5.setCellValue(err.message());
                    c5.setCellStyle(styles.dataLeft);
                }
            }

            sheet.createFreezePane(0, 4);
            sheet.setAutoFilter(new CellRangeAddress(3, Math.max(3, r - 1), 0, headers.length - 1));
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1200, 3600));
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private Map<String, Integer> mapHeaders(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        if (headerRow == null) return map;
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String val = getCellString(headerRow, i).toLowerCase().replace(" ", "_");
                if (!val.isBlank()) {
                    map.put(val, i);
                }
            }
        }
        return map;
    }

    private void validateRequiredHeaders(String sheetName, Map<String, Integer> headerMap, List<String> required, List<ImportRowError> errors) {
        for (String req : required) {
            if (!headerMap.containsKey(req)) {
                errors.add(new ImportRowError(sheetName, 1, req, "", "Sheet '" + sheetName + "' thiếu cột bắt buộc: '" + req + "'."));
            }
        }
    }

    private String getCellString(Row row, Integer colIndex) {
        if (row == null || colIndex == null || colIndex < 0) return "";
        Cell cell = row.getCell(colIndex);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield String.valueOf((long) val);
                }
                yield BigDecimal.valueOf(val).toPlainString();
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue().trim();
                } catch (Exception e) {
                    try {
                        double val = cell.getNumericCellValue();
                        if (val == Math.floor(val) && !Double.isInfinite(val)) {
                            yield String.valueOf((long) val);
                        }
                        yield BigDecimal.valueOf(val).toPlainString();
                    } catch (Exception ex) {
                        yield "";
                    }
                }
            }
            default -> "";
        };
    }

    private boolean isRowEmpty(Row row, int maxCol) {
        if (row == null) return true;
        for (int i = 0; i < maxCol; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && !getCellString(row, i).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private Styles createStyles(Workbook workbook) {
        DataFormat dataFormat = workbook.createDataFormat();

        Font titleFont = workbook.createFont();
        titleFont.setFontName("Calibri");
        titleFont.setFontHeightInPoints((short) 14);
        titleFont.setBold(true);
        titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());

        CellStyle title = workbook.createCellStyle();
        title.setFont(titleFont);
        title.setAlignment(HorizontalAlignment.LEFT);
        title.setVerticalAlignment(VerticalAlignment.CENTER);

        Font headerFont = workbook.createFont();
        headerFont.setFontName("Calibri");
        headerFont.setFontHeightInPoints((short) 11);
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        CellStyle header = workbook.createCellStyle();
        header.setFont(headerFont);
        header.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header.setAlignment(HorizontalAlignment.CENTER);
        header.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(header);

        CellStyle errorHeader = workbook.createCellStyle();
        errorHeader.setFont(headerFont);
        errorHeader.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
        errorHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        errorHeader.setAlignment(HorizontalAlignment.CENTER);
        errorHeader.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(errorHeader);

        CellStyle subHeader = workbook.createCellStyle();
        subHeader.setFont(headerFont);
        subHeader.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        subHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        subHeader.setAlignment(HorizontalAlignment.CENTER);
        subHeader.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(subHeader);

        Font dataFont = workbook.createFont();
        dataFont.setFontName("Calibri");
        dataFont.setFontHeightInPoints((short) 10);

        CellStyle dataLeft = workbook.createCellStyle();
        dataLeft.setFont(dataFont);
        dataLeft.setAlignment(HorizontalAlignment.LEFT);
        dataLeft.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(dataLeft);

        CellStyle dataCenter = workbook.createCellStyle();
        dataCenter.cloneStyleFrom(dataLeft);
        dataCenter.setAlignment(HorizontalAlignment.CENTER);

        CellStyle currency = workbook.createCellStyle();
        currency.cloneStyleFrom(dataLeft);
        currency.setAlignment(HorizontalAlignment.RIGHT);
        currency.setDataFormat(dataFormat.getFormat("#,##0 \"₫\""));

        return new Styles(title, header, errorHeader, subHeader, dataLeft, dataCenter, currency);
    }

    private void setBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
    }

    public record ParsedProduct(
        String productKey,
        String productName,
        Long categoryId,
        String description,
        BigDecimal basePrice,
        ActiveStatus status
    ) {}

    public record ParsedVariant(
        String productKey,
        String sku,
        Long sizeId,
        Long colorId,
        BigDecimal variantPrice,
        ActiveStatus status
    ) {}

    private record Styles(
        CellStyle title,
        CellStyle header,
        CellStyle errorHeader,
        CellStyle subHeader,
        CellStyle dataLeft,
        CellStyle dataCenter,
        CellStyle currency
    ) {}
}

