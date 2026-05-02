package com.drf.inventory.service;

import com.drf.inventory.entity.ProductStock;
import com.drf.inventory.model.request.StockCreateRequest;
import com.drf.inventory.model.response.StockResponse;
import com.drf.inventory.repository.ProductStockRedisRepository;
import com.drf.inventory.repository.ProductStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminStockService {
    private final ProductStockRepository productStockRepository;
    private final ProductStockRedisRepository stockRedisRepository;

    @Transactional(readOnly = true)
    public List<StockResponse> getStocks(List<Long> productIds) {
        return productStockRepository.findAllById(productIds).stream()
                .map(ps -> new StockResponse(ps.getProductId(), ps.getStock()))
                .toList();
    }

    @Transactional
    public void createStock(StockCreateRequest request) {
        ProductStock productStock = ProductStock.create(request.productId(), request.stock());
        productStockRepository.save(productStock);
        stockRedisRepository.setStock(request.productId(), request.stock());
    }
}
