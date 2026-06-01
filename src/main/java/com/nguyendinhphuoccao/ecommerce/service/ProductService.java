package com.nguyendinhphuoccao.ecommerce.service;

import com.nguyendinhphuoccao.ecommerce.dto.ProductRequest;
import com.nguyendinhphuoccao.ecommerce.entity.Product;
import java.util.List;
import java.util.UUID;

public interface ProductService {
    Product createProduct(ProductRequest request);
    Product updateProductTagsAndPrice(UUID productId, ProductRequest request);
    List<Product> getProductsByTag(String tagName); // Thêm dòng này
}
