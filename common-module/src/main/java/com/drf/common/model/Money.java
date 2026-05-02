package com.drf.common.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class Money {

    private static final int KRW_SCALE = 0;
    private static final RoundingMode ROUNDING = RoundingMode.DOWN;

    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = amount.setScale(KRW_SCALE, ROUNDING);
    }


    public static Money of(long amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public Money calculateDiscountAmount(int discountRate) {
        BigDecimal discounted = amount
                .multiply(BigDecimal.valueOf(discountRate))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);

        // 100원 단위 절사
        BigDecimal truncated = discounted
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
                .multiply(BigDecimal.valueOf(100));

        return new Money(truncated);
    }

    public Money subtract(Money other) {
        return new Money(this.amount.subtract(other.amount));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return this.amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros());
    }

    public long toLong() {
        return amount.longValue();
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}