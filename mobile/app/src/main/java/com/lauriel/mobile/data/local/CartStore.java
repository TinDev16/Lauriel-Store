package com.lauriel.mobile.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lauriel.mobile.data.model.Models;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.*;

public final class CartStore {

    public static final class CartItem {

        public long variantId, productId;
        public String productName, sku, size, color, image;
        public BigDecimal displayPrice;
        public int quantity, knownStock;

        public CartItem(
            long variantId,
            long productId,
            String name,
            String sku,
            String size,
            String color,
            String image,
            BigDecimal price,
            int quantity,
            int knownStock
        ) {
            this.variantId = variantId;
            this.productId = productId;
            this.productName = name;
            this.sku = sku;
            this.size = size;
            this.color = color;
            this.image = image;
            this.displayPrice = price;
            this.quantity = quantity;
            this.knownStock = knownStock;
        }
    }

    private static final Type LIST_TYPE = new TypeToken<
        List<CartItem>
    >() {}.getType();
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();
    private final long owner;
    private List<CartItem> items;
    private long revision;
    private long metadataRevision;

    public CartStore(Context context, long userId) {
        if (userId <= 0) throw new IllegalArgumentException(
            "Thiếu mã khách hàng"
        );
        owner = userId;
        prefs = context
            .getApplicationContext()
            .getSharedPreferences("cart_" + userId, Context.MODE_PRIVATE);
        try {
            items = gson.fromJson(prefs.getString("items", "[]"), LIST_TYPE);
        } catch (RuntimeException ignored) {
            items = new ArrayList<>();
        }
        if (items == null) items = new ArrayList<>();
        revision = prefs.getLong("revision", 0);
        metadataRevision = prefs.getLong("metadataRevision", 0);
    }

    public long owner() {
        return owner;
    }

    public synchronized long revision() {
        refresh();
        return revision;
    }

    public synchronized List<CartItem> items() {
        refresh();
        List<CartItem> copy = new ArrayList<>();
        for (CartItem item : items) copy.add(copy(item));
        return copy;
    }

    public synchronized boolean isEmpty() {
        refresh();
        return items.isEmpty();
    }

    public synchronized void add(CartItem item) {
        refresh();
        if (
            item == null ||
            item.variantId <= 0 ||
            item.quantity <= 0 ||
            item.knownStock <= 0
        ) throw new IllegalArgumentException("Sản phẩm không hợp lệ");
        for (CartItem old : items)
            if (old.variantId == item.variantId) {
                int next = Math.addExact(old.quantity, item.quantity);
                if (next > item.knownStock) throw new IllegalArgumentException(
                    "Số lượng vượt quá tồn kho đã biết"
                );
                old.quantity = next;
                old.knownStock = item.knownStock;
                old.displayPrice = item.displayPrice;
                old.image = item.image;
                persist();
                return;
            }
        if (item.quantity > item.knownStock) throw new IllegalArgumentException(
            "Số lượng vượt quá tồn kho đã biết"
        );
        items.add(copy(item));
        persist();
    }

    public synchronized boolean updateImage(long productId, String image) {
        refresh();
        boolean changed = false;
        for (CartItem item : items)
            if (
                item.productId == productId &&
                !Objects.equals(item.image, image)
            ) {
                item.image = image;
                changed = true;
            }
        if (changed) persistImageMetadata();
        return changed;
    }

    public synchronized void setQuantity(long variantId, int quantity) {
        refresh();
        if (quantity <= 0) {
            remove(variantId);
            return;
        }
        for (CartItem item : items)
            if (item.variantId == variantId) {
                if (
                    quantity > item.knownStock
                ) throw new IllegalArgumentException(
                    "Số lượng vượt quá tồn kho đã biết"
                );
                if (item.quantity != quantity) {
                    item.quantity = quantity;
                    persist();
                }
                return;
            }
        throw new IllegalArgumentException("Không tìm thấy sản phẩm trong giỏ");
    }

    public synchronized void remove(long variantId) {
        refresh();
        if (items.removeIf(i -> i.variantId == variantId)) persist();
    }

    public synchronized void clear() {
        refresh();
        items.clear();
        persist();
    }

    public synchronized boolean clearIfRevision(long expected) {
        refresh();
        if (revision != expected) return false;
        clear();
        return true;
    }

    public synchronized List<Models.Item> payload() {
        refresh();
        List<Models.Item> result = new ArrayList<>();
        for (CartItem i : items)
            result.add(new Models.Item(i.variantId, i.quantity));
        return result;
    }

    public synchronized BigDecimal displaySubtotal() {
        refresh();
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem i : items)
            if (i.displayPrice != null) total = total.add(
                i.displayPrice.multiply(BigDecimal.valueOf(i.quantity))
            );
        return total;
    }

    private static CartItem copy(CartItem i) {
        return new CartItem(
            i.variantId,
            i.productId,
            i.productName,
            i.sku,
            i.size,
            i.color,
            i.image,
            i.displayPrice,
            i.quantity,
            i.knownStock
        );
    }

    private void refresh() {
        long stored = prefs.getLong("revision", 0);
        long storedMetadata = prefs.getLong("metadataRevision", 0);
        if (stored == revision && storedMetadata == metadataRevision) return;
        try {
            items = gson.fromJson(prefs.getString("items", "[]"), LIST_TYPE);
        } catch (RuntimeException ignored) {
            items = new ArrayList<>();
        }
        if (items == null) items = new ArrayList<>();
        revision = stored;
        metadataRevision = storedMetadata;
    }

    private void persist() {
        revision++;
        prefs
            .edit()
            .putString("items", gson.toJson(items))
            .putLong("revision", revision)
            .apply();
    }

    private void persistImageMetadata() {
        metadataRevision++;
        prefs
            .edit()
            .putString("items", gson.toJson(items))
            .putLong("metadataRevision", metadataRevision)
            .apply();
    }
}
