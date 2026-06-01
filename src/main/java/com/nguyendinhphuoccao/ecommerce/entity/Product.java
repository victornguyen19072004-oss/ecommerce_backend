package com.nguyendinhphuoccao.ecommerce.entity;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String slug;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(nullable = false)
    private String sku;

    @Column(name = "sale_price", nullable = false)
    private BigDecimal salePrice;

    @Column(name = "compare_price")
    private BigDecimal comparePrice;

    @Column(name = "buying_price", nullable = false)
    private BigDecimal buyingPrice;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 0;

    @Column(name = "short_description", length = 165, nullable = false)
    private String shortDescription;

    @Column(name = "product_description", columnDefinition = "TEXT", nullable = false)
    private String productDescription;

    @Column(name = "product_type", length = 64)
    private String productType; // 'simple' hoặc 'variable'

    @Builder.Default
    private Boolean published = false;

    // Ánh xạ bảng trung gian product_tags
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "product_tags",
        joinColumns = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags;
}