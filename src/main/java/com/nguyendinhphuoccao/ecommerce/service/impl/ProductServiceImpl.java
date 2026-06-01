package com.nguyendinhphuoccao.ecommerce.service.impl;

import com.nguyendinhphuoccao.ecommerce.dto.ProductRequest;
import com.nguyendinhphuoccao.ecommerce.entity.Product;
import com.nguyendinhphuoccao.ecommerce.entity.Tag;
import com.nguyendinhphuoccao.ecommerce.repository.ProductRepository;
import com.nguyendinhphuoccao.ecommerce.repository.TagRepository;
import com.nguyendinhphuoccao.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final TagRepository tagRepository;

    @Override
    @Transactional
    public Product createProduct(ProductRequest request) {
        if (productRepository.existsBySlug(request.getSlug())) {
            throw new RuntimeException("Slug đã tồn tại!");
        }

        Product product = Product.builder()
                .productName(request.getProductName())
                .slug(request.getSlug())
                .sku(request.getSku())
                .salePrice(request.getSalePrice())
                .comparePrice(request.getComparePrice() != null ? request.getComparePrice() : request.getSalePrice())
                .buyingPrice(request.getBuyingPrice())
                .quantity(request.getQuantity())
                .shortDescription(request.getShortDescription())
                .productDescription(request.getProductDescription())
                .productType(request.getProductType() != null ? request.getProductType() : "simple")
                .published(request.getPublished() != null ? request.getPublished() : false)
                .tags(getOrCreateTags(request.getTags()))
                .build();

        return productRepository.save(product);
    }

    @Override
    @Transactional
    public Product updateProductTagsAndPrice(UUID productId, ProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        // Cập nhật giá và tag mới
        product.setSalePrice(request.getSalePrice());
        product.setComparePrice(request.getComparePrice());
        product.setTags(getOrCreateTags(request.getTags()));

        return productRepository.save(product);
    }

    // Hàm bổ trợ xử lý chuyển đổi hoặc tự tạo Tag nếu chưa tồn tại trong DB
    private Set<Tag> getOrCreateTags(java.util.List<String> tagNames) {
        Set<Tag> tags = new HashSet<>();
        if (tagNames != null) {
            for (String tagName : tagNames) {
                Tag tag = tagRepository.findByTagName(tagName.toUpperCase())
                        .orElseGet(() -> tagRepository.save(Tag.builder().tagName(tagName.toUpperCase()).build()));
                tags.add(tag);
            }
        }
        return tags;
    }
}