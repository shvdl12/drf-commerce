package com.drf.inventory.controller;

import com.drf.common.idempotency.Idempotent;
import com.drf.common.model.CommonResponse;
import com.drf.inventory.model.request.StockBatchLookupRequest;
import com.drf.inventory.model.request.StockBatchReleaseRequest;
import com.drf.inventory.model.request.StockBatchReserveRequest;
import com.drf.inventory.model.response.StockResponse;
import com.drf.inventory.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/stocks")
public class InternalStockController {

    private final StockService stockService;

    @GetMapping("/available")
    public ResponseEntity<CommonResponse<List<StockResponse>>> getAvailableStocks(
            @Valid StockBatchLookupRequest request
    ) {
        return ResponseEntity.ok(CommonResponse.success(stockService.getStocks(request.productIds())));
    }

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
