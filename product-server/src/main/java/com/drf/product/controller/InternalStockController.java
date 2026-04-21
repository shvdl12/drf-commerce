package com.drf.product.controller;

import com.drf.common.idempotency.Idempotent;
import com.drf.common.model.CommonResponse;
import com.drf.product.model.request.StockBatchReleaseRequest;
import com.drf.product.model.request.StockBatchReserveRequest;
import com.drf.product.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/stocks")
public class InternalStockController {

    private final StockService stockService;

    @PostMapping("/reserve")
    @Idempotent(scope = "STOCK_RESERVE")
    public ResponseEntity<CommonResponse<Void>> reserveStock(
            @Valid @RequestBody StockBatchReserveRequest request
    ) {
        stockService.batchReserveStock(request);
        return ResponseEntity.ok(CommonResponse.success());
    }

    @PostMapping("/release")
    @Idempotent(scope = "STOCK_RELEASE")
    public ResponseEntity<CommonResponse<Void>> releaseStock(
            @Valid @RequestBody StockBatchReleaseRequest request
    ) {
        stockService.batchReleaseStock(request);
        return ResponseEntity.ok(CommonResponse.success());
    }
}
