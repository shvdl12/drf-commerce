package com.drf.order.service;

import com.drf.common.model.Money;
import org.springframework.stereotype.Component;

@Component
public class DeliveryFeePolicy {
    private static final Money SHIPPING_FEE = Money.of(3000);
    private static final Money FREE_SHIPPING_THRESHOLD = Money.of(50000);

    public Money calculateFee(Money orderAmount) {
        if (isFreeShipping(orderAmount)) {
            return Money.ZERO;
        }
        return SHIPPING_FEE;
    }

    public boolean isFreeShipping(Money orderAmount) {
        return orderAmount.isGreaterThanOrEqualTo(FREE_SHIPPING_THRESHOLD);
    }
}
