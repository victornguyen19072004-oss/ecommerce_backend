package com.nguyendinhphuoccao.ecommerce.dto;

import java.math.BigDecimal;
import java.util.Set;

import lombok.Data;

@Data
public class ProductRequest {
    private String slug;
    private String productName;
    private String sku;
    private BigDecimal salePrice;
    private BigDecimal comparePrice;
    private BigDecimal buyingPrice;
    private Integer quantity;
    private String shortDescription;
    private String productDescription;
    private String productType;
    private Boolean published;
    private Set<String> tagNames; // Chỉ cần truyền mảng tên tag (VD: ["NEW", "SALE"])
}