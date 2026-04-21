package com.drf.order.client;

import com.drf.common.model.CommonResponse;
import com.drf.order.client.dto.request.ProductBatchRequest;
import com.drf.order.client.dto.request.StockReleaseRequest;
import com.drf.order.client.dto.request.StockReserveRequest;
import com.drf.order.client.dto.response.InternalProductResponse;
import com.drf.order.client.dto.response.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "product-client", url = "${clients.product-server.url}")
public interface ProductClient {

    @GetMapping("/internal/products/{id}")
    CommonResponse<ProductResponse> getProduct(@PathVariable("id") long id);

    @PostMapping("/internal/products/batch")
    CommonResponse<List<InternalProductResponse>> getProductsBatch(@RequestBody ProductBatchRequest request);

    @PostMapping("/internal/stocks/{productId}/reserve")
    CommonResponse<Void> reserveStock(
            @PathVariable long productId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody StockReserveRequest request
    );

    @PostMapping("/internal/stocks/{productId}/release")
    CommonResponse<Void> releaseStock(
            @PathVariable long productId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody StockReleaseRequest request
    );
}
