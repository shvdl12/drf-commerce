package com.drf.coupon.discount;

import com.drf.coupon.entity.ApplyType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ApplyScopeRegistry {

    private final Map<ApplyType, ApplyScope> registry;

    public ApplyScopeRegistry(List<ApplyScope> scopes) {
        registry = scopes.stream()
                .collect(Collectors.toMap(ApplyScope::getType, s -> s));
    }

    public ApplyScope get(ApplyType type) {
        return Optional.ofNullable(registry.get(type))
                .orElseThrow(() -> new IllegalArgumentException("Invalid apply scope: : " + type));
    }
}
