package com.drf.product.service;

import com.drf.common.exception.BusinessException;
import com.drf.product.common.exception.ErrorCode;
import com.drf.product.model.request.StockReleaseRequest;
import com.drf.product.model.request.StockReserveRequest;
import com.drf.product.model.response.StockReleaseResponse;
import com.drf.product.model.response.StockReserveResponse;
import com.drf.product.repository.ProductRepository;
import com.drf.product.repository.ProductStockRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockService {
    private final ProductRepository productRepository;
    private final ProductStockRedisRepository stockRedisRepository;

    public StockReserveResponse reserveProductStock(long productId, StockReserveRequest request) {
        if (!productRepository.existsById(productId)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        int result = stockRedisRepository.reserveStock(productId, request.quantity());

        if (result == -1) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (result == -2) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }

        return new StockReserveResponse(productId, result);
    }

    public StockReleaseResponse releaseProductStock(long productId, StockReleaseRequest request) {
        if (!productRepository.existsById(productId)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        int result = stockRedisRepository.releaseStock(productId, request.quantity());

        if (result == -1) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        return new StockReleaseResponse(productId, result);
    }
}
