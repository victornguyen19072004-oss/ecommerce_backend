package com.nguyendinhphuoccao.ecommerce.controller;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nguyendinhphuoccao.ecommerce.dto.ProductRequest;
import com.nguyendinhphuoccao.ecommerce.entity.Product;
import com.nguyendinhphuoccao.ecommerce.entity.Tag;
import com.nguyendinhphuoccao.ecommerce.repository.ProductRepository;
import com.nguyendinhphuoccao.ecommerce.repository.TagRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;
    private final TagRepository tagRepository;

    // 1. Khởi tạo Tags
    @PostMapping("/tags")
    public ResponseEntity<?> createTag(@RequestParam String name) {
        Tag tag = tagRepository.findByTagName(name)
                .orElseGet(() -> tagRepository.save(Tag.builder().tagName(name).build()));
        return ResponseEntity.ok(tag);
    }

    // 2. Thêm mới Product
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody ProductRequest req) {
        Set<Tag> tags = new HashSet<>();
        if (req.getTagNames() != null) {
            req.getTagNames().forEach(name -> {
                tags.add(tagRepository.findByTagName(name).orElseGet(() -> 
                        tagRepository.save(Tag.builder().tagName(name).build())));
            });
        }

        Product product = Product.builder()
                .slug(req.getSlug())
                .productName(req.getProductName())
                .sku(req.getSku())
                .salePrice(req.getSalePrice())
                .comparePrice(req.getComparePrice())
                .buyingPrice(req.getBuyingPrice())
                .quantity(req.getQuantity())
                .shortDescription(req.getShortDescription())
                .productDescription(req.getProductDescription())
                .productType(req.getProductType())
                .published(req.getPublished())
                .tags(tags)
                .build();

        return ResponseEntity.ok(productRepository.save(product));
    }

    // 3. Cập nhật Product (Đổi tag, giảm giá)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable UUID id, @RequestBody ProductRequest req) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        // Cập nhật giá
        product.setSalePrice(req.getSalePrice());
        product.setComparePrice(req.getComparePrice());

        // Cập nhật Tag
        Set<Tag> tags = new HashSet<>();
        if (req.getTagNames() != null) {
            req.getTagNames().forEach(name -> {
                tags.add(tagRepository.findByTagName(name).orElseGet(() -> 
                        tagRepository.save(Tag.builder().tagName(name).build())));
            });
        }
        product.setTags(tags);

        return ResponseEntity.ok(productRepository.save(product));
    }
}