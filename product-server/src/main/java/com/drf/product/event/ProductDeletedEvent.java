package com.drf.product.event;

import com.drf.common.event.BaseEvent;

public class ProductDeletedEvent extends BaseEvent<ProductDeletedEvent.Payload> {

    public ProductDeletedEvent(long id) {
        super(ProductEventType.PRODUCT_DELETED.name(), new Payload(id));
    }

    public record Payload(long id) {
    }
}
