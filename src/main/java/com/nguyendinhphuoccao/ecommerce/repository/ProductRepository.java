package com.nguyendinhphuoccao.ecommerce.repository;

import com.nguyendinhphuoccao.ecommerce.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    boolean existsBySlug(String slug);
    // Tìm danh sách sản phẩm dựa vào tên Tag (Ví dụ: "NEW" hoặc "SALE")
    List<Product> findByTags_TagName(String tagName);
}
