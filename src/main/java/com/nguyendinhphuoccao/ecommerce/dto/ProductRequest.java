package com.nguyendinhphuoccao.ecommerce.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

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
}
