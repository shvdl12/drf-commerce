package com.drf.inventory.service;

import com.drf.common.exception.BusinessException;
import com.drf.inventory.common.exception.ErrorCode;
import com.drf.inventory.model.request.StockBatchReleaseRequest;
import com.drf.inventory.model.request.StockBatchReserveRequest;
import com.drf.inventory.model.response.StockResponse;
import com.drf.inventory.repository.ProductStockRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {
    private final ProductStockRedisRepository stockRedisRepository;

    public List<StockResponse> getStocks(List<Long> productIds) {
        List<Long> stocks = stockRedisRepository.getStocks(productIds);

        return IntStream.range(0, productIds.size())
                .filter(i -> stocks.get(i) != null)
                .mapToObj(i -> new StockResponse(productIds.get(i), stocks.get(i)))
                .toList();
    }

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

    private void reserveProductStock(long productId, long quantity) {
        int result = stockRedisRepository.reserveStock(productId, quantity);

        if (result == -1) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (result == -2) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
    }

    private void releaseProductStock(long productId, long quantity) {
        int result = stockRedisRepository.releaseStock(productId, quantity);

        if (result == -1) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
    }
}
