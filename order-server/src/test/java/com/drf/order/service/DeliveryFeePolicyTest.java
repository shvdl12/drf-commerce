package com.drf.order.service;

import com.drf.common.model.Money;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

public class DeliveryFeePolicyTest {

    private final DeliveryFeePolicy deliveryFeePolicy = new DeliveryFeePolicy();

    @ParameterizedTest
    @CsvSource({
            "49999, 3000",
            "50000, 0",
            "50001, 0"
    })
    void calculateFee_shouldReturnExpectedFee(long orderAmount, long expectedFee) {
        Money result = deliveryFeePolicy.calculateFee(Money.of(orderAmount));

        assertThat(result).isEqualTo(Money.of(expectedFee));
    }
}