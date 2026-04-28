package com.drf.coupon.discount;

import com.drf.coupon.entity.ApplyScope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ApplyScopeRegistry {

    private final Map<ApplyScope, ApplyScopeStrategy> registry;

    public ApplyScopeRegistry(List<ApplyScopeStrategy> strategies) {
        registry = strategies.stream()
                .collect(Collectors.toMap(ApplyScopeStrategy::getApplyScope, s -> s));
    }

    public ApplyScopeStrategy get(ApplyScope applyScope) {
        return Optional.ofNullable(registry.get(applyScope))
                .orElseThrow(() -> new IllegalArgumentException("Invalid apply scope: " + applyScope));
    }
}
