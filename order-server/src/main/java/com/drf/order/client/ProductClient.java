package com.drf.order.client;

import com.drf.common.model.CommonResponse;
import com.drf.order.client.dto.request.ProductBatchRequest;
import com.drf.order.client.dto.response.InternalProductResponse;
import com.drf.order.client.dto.response.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "product-client", url = "${clients.product-server.url}")
public interface ProductClient {

    @GetMapping("/internal/products/{id}")
    CommonResponse<ProductResponse> getProduct(@PathVariable("id") long id);

    @PostMapping("/internal/products/batch")
    CommonResponse<List<InternalProductResponse>> getProductsBatch(@RequestBody ProductBatchRequest request);

}
