package com.drf.product.event.handler;

import com.drf.product.event.ProductCreatedEvent;
import com.drf.product.event.ProductDeletedEvent;
import com.drf.product.event.ProductUpdatedEvent;
import com.drf.product.repository.ProductStockRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventHandler {
    private final ProductStockRedisRepository productStockRedisRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCreatedProductEvent(ProductCreatedEvent event) {
        ProductCreatedEvent.Payload payload = event.getPayload();
        setStock(payload.id(), payload.stock());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUpdatedProductEvent(ProductUpdatedEvent event) {
        ProductUpdatedEvent.Payload payload = event.getPayload();
        setStock(payload.id(), payload.stock());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDeletedProductEvent(ProductDeletedEvent event) {
        productStockRedisRepository.deleteStock(event.getPayload().id());
    }

    private void setStock(long productId, int stock) {
        try {
            productStockRedisRepository.setStock(productId, stock);
        } catch (Exception e) {
            log.error("Failed to sync stock to Redis - productId: {}, stock: {}", productId, stock, e);
        }
    }
}
