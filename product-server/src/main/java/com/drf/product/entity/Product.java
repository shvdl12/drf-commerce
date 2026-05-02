package com.drf.product.entity;

import com.drf.common.converter.MoneyConverter;
import com.drf.common.entity.BaseTimeEntity;
import com.drf.common.model.Money;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "product")
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 100)
    private String name;

    @Convert(converter = MoneyConverter.class)
    @Column(nullable = false)
    private Money price;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    @Column(nullable = false)
    private int discountRate;

    private LocalDateTime saleStartAt;
    private LocalDateTime saleEndAt;

    private LocalDateTime deletedAt;

    public static Product create(Category category, String name, long price, String description,
                                 Integer discountRate, LocalDateTime saleStartAt, LocalDateTime saleEndAt) {
        return Product.builder()
                .category(category)
                .name(name)
                .price(Money.of(price))
                .description(description)
                .status(ProductStatus.READY)
                .discountRate(Objects.requireNonNullElse(discountRate, 0))
                .saleStartAt(saleStartAt)
                .saleEndAt(saleEndAt)
                .build();
    }

    public void updateProduct(Category category, String name, Long price, String description,
                              Integer discountRate, LocalDateTime saleStartAt, LocalDateTime saleEndAt) {
        if (category != null) this.category = category;
        if (StringUtils.hasText(name)) this.name = name;
        if (price != null) this.price = Money.of(price);
        if (StringUtils.hasText(description)) this.description = description;
        if (discountRate != null) this.discountRate = discountRate;
        if (saleStartAt != null) this.saleStartAt = saleStartAt;
        if (saleEndAt != null) this.saleEndAt = saleEndAt;
    }

    public void delete() {
        this.status = ProductStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    public Money calculateDiscountAmount() {
        return this.price.calculateDiscountAmount(this.discountRate);
    }

    public Money calculateDiscountedPrice() {
        return this.price.subtract(calculateDiscountAmount());
    }
}
