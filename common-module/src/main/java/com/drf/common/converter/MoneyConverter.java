package com.drf.common.converter;

import com.drf.common.model.Money;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MoneyConverter implements AttributeConverter<Money, Long> {

    @Override
    public Long convertToDatabaseColumn(Money money) {
        return money == null ? null : money.toLong();
    }

    @Override
    public Money convertToEntityAttribute(Long dbValue) {
        return dbValue == null ? null : Money.of(dbValue);
    }
}
