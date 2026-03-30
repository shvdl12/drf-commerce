package com.drf.product.controller;

import com.drf.common.model.CommonResponse;
import com.drf.product.model.request.StockReleaseRequest;
import com.drf.product.model.request.StockReserveRequest;
import com.drf.product.model.response.StockReleaseResponse;
import com.drf.product.model.response.StockReserveResponse;
import com.drf.product.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @PostMapping("/stocks/{productId}/reserve")
    public ResponseEntity<CommonResponse<StockReserveResponse>> reserveStock(
            @PathVariable long productId,
            @Valid @RequestBody StockReserveRequest request
    ) {
        StockReserveResponse response = stockService.reserveProductStock(productId, request);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @PostMapping("/stocks/{productId}/release")
    public ResponseEntity<CommonResponse<StockReleaseResponse>> releaseStock(
            @PathVariable long productId,
            @Valid @RequestBody StockReleaseRequest request
    ) {
        StockReleaseResponse response = stockService.releaseProductStock(productId, request);
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}
