package com.drf.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class Money {

    public static final Money ZERO = new Money(BigDecimal.ZERO);
    private static final int KRW_SCALE = 0;
    private static final RoundingMode ROUNDING = RoundingMode.DOWN;
    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = amount.setScale(KRW_SCALE, ROUNDING);
    }

    public static Money of(int amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    public static Money of(long amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money subtract(Money other) {
        return new Money(this.amount.subtract(other.amount).max(BigDecimal.ZERO));
    }

    public Money multiply(int factor) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)));
    }

    public Money percent(int rate) {
        return new Money(
                this.amount.multiply(BigDecimal.valueOf(rate))
                        .divide(BigDecimal.valueOf(100), KRW_SCALE, ROUNDING)
        );
    }

    public Money proportionOf(Money itemAmount, Money totalAmount) {
        if (totalAmount.isZero()) return ZERO;
        return new Money(
                this.amount
                        .multiply(itemAmount.amount)
                        .divide(totalAmount.amount, KRW_SCALE, ROUNDING)
        );
    }

    public Money truncateTo(int unit) {
        BigDecimal u = BigDecimal.valueOf(unit);
        return new Money(this.amount.divide(u, 0, RoundingMode.DOWN).multiply(u));
    }

    public Money cap(Money max) {
        return new Money(this.amount.min(max.amount));
    }

    public boolean isGreaterThanOrEqualTo(Money other) {
        return this.amount.compareTo(other.amount) >= 0;
    }

    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public int value() {
        return this.amount.intValue();
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
