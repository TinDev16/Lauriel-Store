package com.lauriel.mobile.data.model;

import java.math.BigDecimal;
import java.util.List;

public final class Models {
    private Models() {}
    public static final class LoginRequest {
        public String username, password;
        public LoginRequest(String username,String password){this.username=username;this.password=password;}
    }
    public static final class RegisterRequest {
        public String username,email,password,fullName,phone;
        public RegisterRequest(String username,String email,String password,String fullName,String phone){
            this.username=username;this.email=email;this.password=password;this.fullName=fullName;this.phone=phone;
        }
    }
    public static final class LoginResponse { public String token,tokenType,username,role; public long expiresIn,userId; }
    public static final class RegisterResponse { public long userId; public String username,email,role,message; }
    public static final class Me { public long userId; public String username,role; }
    public static final class Page<T> { public List<T> content; public int page,size,totalPages; public long totalElements; }
    public static final class Size { public long sizeId; public String sizeName; }
    public static final class Color { public long colorId; public String colorName; }
    public static final class Promotion { public long promotionId; public String promotionName,description,startDate,endDate,status; public BigDecimal discountPercent; }
    public static final class PromotionInfo { public long promotionId; public String promotionName,startDate,endDate; public BigDecimal discountPercent; }
    public static final class Product {
        public long productId; public String productName,description,imgUrl,status;
        public BigDecimal price,basePrice; public List<Variant> variants;
    }
    public static final class Variant {
        public long variantId,productId; public String sku,status;
        public Size size; public Color color; public int quantity;
        public BigDecimal price,effectivePrice; public PromotionInfo promotion;
    }
    public static final class Item {
        public long variantId; public int quantity;
        public Item(long variantId,int quantity){this.variantId=variantId;this.quantity=quantity;}
    }
    public static final class VoucherRequest {
        public String code; public List<Item> items;
        public VoucherRequest(String code,List<Item> items){this.code=code;this.items=items;}
    }
    public static final class VoucherPreview {
        public boolean valid; public String code; public BigDecimal orderAmount,discountAmount,finalAmount;
    }
    public static final class CheckoutRequest {
        public List<Item> items; public String shippingAddress,voucherCode;
        public CheckoutRequest(List<Item> items,String shippingAddress,String voucherCode){
            this.items=items;this.shippingAddress=shippingAddress;this.voucherCode=voucherCode;
        }
    }
    public static final class OrderSummary {
        public long orderId,customerId; public String status,createdAt;
        public BigDecimal totalAmount,discountAmount,finalAmount;
    }
    public static final class OrderLine {
        public long variantId; public String sku,productName;
        public Size size; public Color color; public int quantity;
        public BigDecimal unitPrice,discountAmount,subtotal;
    }
    public static final class Order {
        public long orderId,customerId; public String status,createdAt,updatedAt,shippingAddress,voucherCode;
        public BigDecimal totalAmount,discountAmount,finalAmount; public List<OrderLine> items;
    }
    public static final class ApiError { public int status; public String error,message; }
}
