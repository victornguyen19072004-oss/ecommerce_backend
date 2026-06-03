package com.nguyendinhphuoccao.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
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
    private Integer quantity;

    @Column(name = "short_description", length = 165, nullable = false)
    private String shortDescription;

    @Column(name = "product_description", columnDefinition = "TEXT", nullable = false)
    private String productDescription;

    @Column(name = "product_type", length = 64)
    private String productType;

    @Column(nullable = false)
    private Boolean published;

    // Quan hệ Nhiều-Nhiều sinh ra bảng product_tags
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "product_tags",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "product_categories",
        joinColumns = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();
}
