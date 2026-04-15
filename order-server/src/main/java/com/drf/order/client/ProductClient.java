package com.drf.order.client;

import com.drf.common.model.CommonResponse;
import com.drf.order.client.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-client", url = "${clients.product-server.url}")
public interface ProductClient {

    @GetMapping("/internal/products/{id}")
    CommonResponse<ProductResponse> getProduct(@PathVariable("id") long id);
}
