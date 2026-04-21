package com.drf.product.service;

import com.drf.common.exception.BusinessException;
import com.drf.product.common.exception.ErrorCode;
import com.drf.product.model.request.StockBatchReleaseRequest;
import com.drf.product.model.request.StockBatchReserveRequest;
import com.drf.product.repository.ProductRepository;
import com.drf.product.repository.ProductStockRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {
    private final ProductRepository productRepository;
    private final ProductStockRedisRepository stockRedisRepository;

    public void batchReserveStock(StockBatchReserveRequest request) {
        var reserved = new ArrayList<StockBatchReserveRequest.StockBatchReserveItem>();
        for (var item : request.items()) {
            try {
                reserveProductStock(item.productId(), item.quantity());
                reserved.add(item);
            } catch (Exception e) {
                reserved.forEach(r -> {
                    try {
                        releaseProductStock(r.productId(), item.quantity());
                    } catch (Exception ex) {
                        log.warn("Compensation release failed for productId={}", r.productId(), ex);
                    }
                });
                throw e;
            }
        }
    }

    public void batchReleaseStock(StockBatchReleaseRequest request) {
        for (var item : request.items()) {
            try {
                releaseProductStock(item.productId(), item.quantity());
            } catch (Exception e) {
                log.error("Release failed for productId={}", item.productId(), e);
            }
        }
    }

    private void reserveProductStock(long productId, int quantity) {
        if (!productRepository.existsById(productId)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        int result = stockRedisRepository.reserveStock(productId, quantity);

        if (result == -1) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (result == -2) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
    }

    private void releaseProductStock(long productId, int quantity) {
        if (!productRepository.existsById(productId)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        int result = stockRedisRepository.releaseStock(productId, quantity);

        if (result == -1) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
    }
}
