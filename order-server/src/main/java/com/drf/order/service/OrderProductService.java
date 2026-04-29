package com.drf.order.service;

import com.drf.common.exception.BusinessException;
import com.drf.order.client.ProductClient;
import com.drf.order.client.dto.request.ProductBatchRequest;
import com.drf.order.client.dto.request.StockBatchReleaseRequest;
import com.drf.order.client.dto.request.StockBatchReserveRequest;
import com.drf.order.client.dto.response.InternalProductResponse;
import com.drf.order.common.exception.ErrorCode;
import com.drf.order.entity.CartItem;
import com.drf.order.model.dto.OrderLineItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProductService {
    private static final String STOCK_RESERVE_KEY_SUFFIX = ":STOCK_RESERVE";
    private static final String STOCK_RELEASE_KEY_SUFFIX = ":STOCK_RELEASE";


    private final ProductClient productClient;

    public List<OrderLineItem> getOrderLineItems(List<CartItem> cartItems) {
        List<Long> productIds = cartItems.stream().map(CartItem::getProductId).toList();

        Map<Long, InternalProductResponse> productMap = new HashMap<>();
        ProductBatchRequest productBatchRequest = new ProductBatchRequest(productIds);

        for (InternalProductResponse p : productClient.getProductsBatch(productBatchRequest).getData()) {
            productMap.put(p.id(), p);
        }

        List<OrderLineItem> orderLineItems = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            InternalProductResponse product = productMap.get(cartItem.getProductId());
            if (product == null) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            orderLineItems.add(OrderLineItem.of(cartItem, product));
        }

        return orderLineItems;
    }

    public void reserveStocks(List<OrderLineItem> lineItems, String idempotencyKey) {
        productClient.reserveStock(idempotencyKey + STOCK_RESERVE_KEY_SUFFIX,
                new StockBatchReserveRequest(lineItems.stream()
                        .map(item -> new StockBatchReserveRequest.StockBatchReserveItem(item.getProductId(), item.getQuantity()))
                        .toList()));
    }

    public void releaseStocks(List<OrderLineItem> lineItems, String idempotencyKey) {
        try {
            productClient.releaseStock(idempotencyKey + STOCK_RELEASE_KEY_SUFFIX,
                    new StockBatchReleaseRequest(lineItems.stream()
                            .map(item -> new StockBatchReleaseRequest.StockBatchReleaseItem(item.getProductId(), item.getQuantity()))
                            .toList()));
        } catch (Exception e) {
            log.error("Stock batch release failed", e);
        }
    }
}
