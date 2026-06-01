package com.nguyendinhphuoccao.ecommerce.controller;

import com.nguyendinhphuoccao.ecommerce.dto.ProductRequest;
import com.nguyendinhphuoccao.ecommerce.entity.Product; // BỔ SUNG DÒNG NÀY
import com.nguyendinhphuoccao.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable UUID id, @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProductTagsAndPrice(id, request));
    }

    @GetMapping("/tag/{tagName}")
    public ResponseEntity<java.util.List<Product>> getProductsByTag(@PathVariable String tagName) {
        return ResponseEntity.ok(productService.getProductsByTag(tagName));
    }
}
