package com.nguyendinhphuoccao.ecommerce.service;

import java.util.UUID;

import com.nguyendinhphuoccao.ecommerce.dto.ProductRequest;
import com.nguyendinhphuoccao.ecommerce.entity.Product;

public interface ProductService {
    Product createProduct(ProductRequest request);
    Product updateProductTagsAndPrice(UUID productId, ProductRequest request);
}
