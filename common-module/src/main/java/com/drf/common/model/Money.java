package com.drf.common.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class Money {
    private static final int KRW_SCALE = 0;
    private static final RoundingMode ROUNDING = RoundingMode.DOWN;

    public static final Money ZERO = new Money(BigDecimal.ZERO);

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

    public boolean isGreaterThanOrEqualTo(Money other) {
        return this.amount.compareTo(other.amount) >= 0;
    }

    public boolean isLessThanOrEqualTo(Money other) {
        return this.amount.compareTo(other.amount) <= 0;
    }

    public Money add(Money other) {
        other = defaultIfNull(other);
        return new Money(this.amount.add(other.amount));
    }

    public Money multiply(int factor) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)));
    }

    public Money percent(long rate) {
        return new Money(
                this.amount.multiply(BigDecimal.valueOf(rate))
                        .divide(BigDecimal.valueOf(100), KRW_SCALE, ROUNDING)
        );
    }

    public Money truncateTo(int unit) {
        BigDecimal u = BigDecimal.valueOf(unit);
        return new Money(this.amount.divide(u, 0, RoundingMode.DOWN).multiply(u));
    }

    public Money min(Money other) {
        return new Money(this.amount.min(other.amount));
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
        other = defaultIfNull(other);
        return new Money(this.amount.subtract(other.amount));
    }

    public Money proportionOf(Money itemAmount, Money totalAmount) {
        if (totalAmount.isZero()) return ZERO;
        return new Money(
                this.amount
                        .multiply(itemAmount.amount)
                        .divide(totalAmount.amount, KRW_SCALE, ROUNDING)
        );
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public long toLong() {
        return amount.longValue();
    }

    private Money defaultIfNull(Money money) {
        return money == null ? Money.ZERO : money;
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

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}