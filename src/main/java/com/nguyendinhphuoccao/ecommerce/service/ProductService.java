package com.nguyendinhphuoccao.ecommerce.service;

import java.util.List;
import java.util.UUID;

import com.nguyendinhphuoccao.ecommerce.dto.ProductRequest;
import com.nguyendinhphuoccao.ecommerce.entity.Product;

public interface ProductService {
    Product createProduct(ProductRequest request);
    Product updateProductTagsAndPrice(UUID productId, ProductRequest request);

    List<Product> getProductsByTag(String tagName); // Thêm dòng này
    
    // BỔ SUNG DÒNG NÀY
    List<Product> getProductsByCategory(String categoryName);
}
