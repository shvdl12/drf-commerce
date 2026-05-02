package com.drf.coupon.strategy.discount;

import com.drf.coupon.entity.DiscountType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class DiscountStrategyRegistry {

    private final Map<DiscountType, DiscountStrategy> registry;

    public DiscountStrategyRegistry(List<DiscountStrategy> policies) {
        registry = policies.stream()
                .collect(Collectors.toMap(DiscountStrategy::getType, p -> p));
    }

    public DiscountStrategy get(DiscountType type) {
        return Optional.ofNullable(registry.get(type))
                .orElseThrow(() -> new IllegalArgumentException("Invalid discount type: " + type));
    }
}
