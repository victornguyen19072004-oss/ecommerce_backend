package com.nguyendinhphuoccao.ecommerce.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nguyendinhphuoccao.ecommerce.dto.ProductRequest;
import com.nguyendinhphuoccao.ecommerce.entity.Category;
import com.nguyendinhphuoccao.ecommerce.entity.Product;
import com.nguyendinhphuoccao.ecommerce.entity.Tag;
import com.nguyendinhphuoccao.ecommerce.repository.CategoryRepository;
import com.nguyendinhphuoccao.ecommerce.repository.ProductRepository;
import com.nguyendinhphuoccao.ecommerce.repository.TagRepository;
import com.nguyendinhphuoccao.ecommerce.service.ProductService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final TagRepository tagRepository;
    private final CategoryRepository categoryRepository;

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
                .categories(getOrCreateCategories(request.getCategories()))
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

    @Override
    @Transactional(readOnly = true)
    public List<Product> getProductsByTag(String tagName) {
        return productRepository.findByTags_TagName(tagName.toUpperCase());
    }
    
    // --- BỔ SUNG HÀM LẤY SẢN PHẨM THEO CATEGORY ---
    @Override
    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(String categoryName) {
        // Xử lý đặc thù cho danh mục "Tops"
        if ("Tops".equalsIgnoreCase(categoryName)) {
            // Danh sách 5 danh mục con thuộc Tops
            List<String> topSubCategories = Arrays.asList(
                "Shirts & Blouses", 
                "Cardigans & Sweaters", 
                "Knitwear", 
                "Blazers", 
                "Outerwear"
            );
            
            List<Product> topsProducts = new ArrayList<>();
            
            for (String subCat : topSubCategories) {
                // Lấy tất cả sản phẩm của từng danh mục con
                List<Product> productsInCat = productRepository.findByCategories_CategoryNameIgnoreCase(subCat);
                
                // Đảo vị trí ngẫu nhiên
                Collections.shuffle(productsInCat);
                
                // Trích xuất đúng 2 sản phẩm và thêm vào danh sách Tops
                topsProducts.addAll(productsInCat.stream().limit(2).collect(Collectors.toList()));
            }
            
            // Đảo vị trí danh sách Tops một lần nữa để 10 sản phẩm hiển thị xen kẽ đẹp mắt
            Collections.shuffle(topsProducts);
            return topsProducts;
        }
        
        // Trả về bình thường cho các danh mục khác (Pants, Jeans, Skirts...)
        return productRepository.findByCategories_CategoryNameIgnoreCase(categoryName);
    }

    // --- CÁC HÀM BỔ TRỢ (PRIVATE) ---

    // Hàm bổ trợ xử lý chuyển đổi hoặc tự tạo Category
    private Set<Category> getOrCreateCategories(List<String> categoryNames) {
        Set<Category> categories = new HashSet<>();
        if (categoryNames != null && !categoryNames.isEmpty()) {
            for (String catName : categoryNames) {
                Category category = categoryRepository.findByCategoryNameIgnoreCase(catName)
                        .orElseGet(() -> categoryRepository.save(Category.builder().categoryName(catName).active(true).build()));
                categories.add(category);
            }
        }
        return categories;
    }

    // Hàm bổ trợ xử lý chuyển đổi hoặc tự tạo Tag
    private Set<Tag> getOrCreateTags(List<String> tagNames) {
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
