package com.nguyendinhphuoccao.ecommerce.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nguyendinhphuoccao.ecommerce.entity.Product;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    boolean existsBySlug(String slug);
    // Tìm danh sách sản phẩm dựa vào tên Tag (Ví dụ: "NEW" hoặc "SALE")
    List<Product> findByTags_TagName(String tagName);

    // BỔ SUNG DÒNG NÀY: Tìm sản phẩm theo tên danh mục (không phân biệt hoa thường)
    List<Product> findByCategories_CategoryNameIgnoreCase(String categoryName);
}
