package com.drf.product.event.consumer;

import com.drf.product.repository.ProductStockRedisRepository;
import com.drf.product.repository.ProductStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventProcessor {

    private final ProcessedEventRepository processedEventRepository;
    private final ProductStockRepository productStockRepository;
    private final ProductStockRedisRepository stockRedisRepository;

    @Transactional
    public void processPaymentCompleted(long eventId, long productId, int quantity) {
        processedEventRepository.save(ProcessedEvent.of(eventId, "PAYMENT_COMPLETED"));
        int updated = productStockRepository.decrementStock(productId, quantity);
        if (updated == 0) {
            log.error("processPaymentCompleted - failed (not found or insufficient stock). productId={}, quantity={}", productId, quantity);
        }
    }

    @Transactional
    public void processRefundCompleted(long eventId, long productId, int quantity) {
        processedEventRepository.save(ProcessedEvent.of(eventId, "REFUND_COMPLETED"));
        int redisResult = stockRedisRepository.releaseStock(productId, quantity);
        if (redisResult == -1) {
            log.warn("processRefundCompleted - Redis stock key not found. productId={}", productId);
        }
        int updated = productStockRepository.incrementStock(productId, quantity);
        if (updated == 0) {
            log.warn("processRefundCompleted - ProductStock not found. productId={}", productId);
        }
    }
}
