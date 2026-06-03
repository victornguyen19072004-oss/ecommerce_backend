package com.nguyendinhphuoccao.ecommerce.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class ProductRequest {
    private String productName;
    private String slug;
    private String sku;
    private BigDecimal salePrice;
    private BigDecimal comparePrice;
    private BigDecimal buyingPrice;
    private Integer quantity;
    private String shortDescription;
    private String productDescription;
    private String productType;
    private Boolean published;
    private List<String> tags; // Chứa danh sách tên tag: ["NEW"], hoặc ["SALE"]
    private List<String> categories; // Tên các danh mục cần liên kết
}
