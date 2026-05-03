package com.drf.order.service;

import com.drf.common.model.Money;
import com.drf.order.client.ProductClient;
import com.drf.order.client.dto.request.ProductBatchRequest;
import com.drf.order.client.dto.response.InternalProductResponse;
import com.drf.order.model.request.CheckoutItemRequest;
import com.drf.order.model.response.CheckoutAvailableItem;
import com.drf.order.model.response.CheckoutResponse;
import com.drf.order.model.response.CheckoutUnavailableItem;
import com.drf.order.model.response.CheckoutUnavailableItem.UnavailableReason;
import com.drf.order.model.type.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final ProductClient productClient;
    private final DeliveryFeePolicy deliveryFeePolicy;

    public CheckoutResponse checkout(List<CheckoutItemRequest> items) {
        ProductBatchRequest request = new ProductBatchRequest(items.stream().map(CheckoutItemRequest::productId).toList());
        Map<Long, InternalProductResponse> productMap = productClient.getProductsBatch(request).getData()
                .stream()
                .collect(Collectors.toMap(InternalProductResponse::id, Function.identity()));

        List<CheckoutAvailableItem> availableItems = new ArrayList<>();
        List<CheckoutUnavailableItem> unavailableItems = new ArrayList<>();

        Money orderAmount = Money.ZERO;

        for (CheckoutItemRequest item : items) {
            InternalProductResponse product = productMap.get(item.productId());

            if (product == null || product.status() != ProductStatus.ON_SALE) {
                String name = product != null ? product.name() : "알 수 없는 상품";
                unavailableItems.add(CheckoutUnavailableItem.of(item.productId(), name, UnavailableReason.NOT_ON_SALE));
                continue;
            }

            if (product.stock() == 0) {
                unavailableItems.add(CheckoutUnavailableItem.of(product.id(), product.name(), UnavailableReason.OUT_OF_STOCK));
                continue;
            }

            if (product.stock() < item.quantity()) {
                unavailableItems.add(CheckoutUnavailableItem.of(product.id(), product.name(), UnavailableReason.INSUFFICIENT_STOCK));
                continue;
            }

            Money subtotal = Money.of(product.discountedPrice()).multiply(item.quantity());
            orderAmount = orderAmount.add(subtotal);

            availableItems.add(CheckoutAvailableItem.of(product, item.quantity(), subtotal));
        }

        Money deliveryFee = availableItems.isEmpty() ? Money.ZERO : deliveryFeePolicy.calculateFee(orderAmount);

        return CheckoutResponse.of(availableItems, unavailableItems, orderAmount, deliveryFee, orderAmount.add(deliveryFee));
    }
}
