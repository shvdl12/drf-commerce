package com.drf.coupon.discount;

import com.drf.coupon.entity.DiscountType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class DiscountPolicyRegistry {

    private final Map<DiscountType, DiscountPolicy> registry;

    public DiscountPolicyRegistry(List<DiscountPolicy> policies) {
        registry = policies.stream()
                .collect(Collectors.toMap(DiscountPolicy::getType, p -> p));
    }

    public DiscountPolicy get(DiscountType type) {
        return Optional.ofNullable(registry.get(type))
                .orElseThrow(() -> new IllegalArgumentException("Invalid discount type: " + type));
    }
}
