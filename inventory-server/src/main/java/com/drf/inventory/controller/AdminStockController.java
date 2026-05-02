package com.drf.inventory.controller;

import com.drf.common.model.CommonResponse;
import com.drf.inventory.model.request.StockBatchLookupRequest;
import com.drf.inventory.model.request.StockCreateRequest;
import com.drf.inventory.model.response.StockResponse;
import com.drf.inventory.service.AdminStockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/stocks")
public class AdminStockController {

    private final AdminStockService adminStockService;

    @GetMapping("/confirmed")
    public ResponseEntity<CommonResponse<List<StockResponse>>> getConfirmedStocks(
            @Valid StockBatchLookupRequest request
    ) {
        return ResponseEntity.ok(CommonResponse.success(adminStockService.getStocks(request.productIds())));
    }

    @PostMapping
    public ResponseEntity<CommonResponse<Void>> createStock(
            @Valid @RequestBody StockCreateRequest request
    ) {
        adminStockService.createStock(request);
        return ResponseEntity.ok(CommonResponse.success());
    }
}
