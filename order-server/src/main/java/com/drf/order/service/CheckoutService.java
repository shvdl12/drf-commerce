package com.drf.order.service;

import com.drf.order.client.ProductClient;
import com.drf.order.client.dto.request.ProductBatchRequest;
import com.drf.order.client.dto.response.InternalProductResponse;
import com.drf.order.model.request.CheckoutItemRequest;
import com.drf.order.model.response.CheckoutAvailableItem;
import com.drf.order.model.response.CheckoutResponse;
import com.drf.order.model.response.CheckoutUnavailableItem;
import com.drf.order.model.response.CheckoutUnavailableItem.UnavailableReason;
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

    private static final int FREE_SHIPPING_THRESHOLD = 50000;
    private static final int SHIPPING_FEE = 3000;

    private final ProductClient productClient;

    public CheckoutResponse checkout(List<CheckoutItemRequest> items) {
        ProductBatchRequest request = new ProductBatchRequest(items.stream().map(CheckoutItemRequest::productId).toList());
        Map<Long, InternalProductResponse> productMap = productClient.getProductsBatch(request).getData()
                .stream()
                .collect(Collectors.toMap(InternalProductResponse::id, Function.identity()));

        List<CheckoutAvailableItem> availableItems = new ArrayList<>();
        List<CheckoutUnavailableItem> unavailableItems = new ArrayList<>();

        for (CheckoutItemRequest item : items) {
            InternalProductResponse product = productMap.get(item.productId());

            if (product == null || !"ON_SALE".equals(product.status())) {
                String name = product != null ? product.name() : "알 수 없는 상품";
                unavailableItems.add(new CheckoutUnavailableItem(item.productId(), name, UnavailableReason.NOT_ON_SALE));
                continue;
            }

            if (product.stock() == 0) {
                unavailableItems.add(new CheckoutUnavailableItem(product.id(), product.name(), UnavailableReason.OUT_OF_STOCK));
                continue;
            }

            if (product.stock() < item.quantity()) {
                unavailableItems.add(new CheckoutUnavailableItem(product.id(), product.name(), UnavailableReason.INSUFFICIENT_STOCK));
                continue;
            }

            availableItems.add(new CheckoutAvailableItem(
                    product.id(),
                    product.name(),
                    product.price(),
                    product.discountedPrice(),
                    item.quantity(),
                    product.discountedPrice() * item.quantity()
            ));
        }

        int itemTotal = availableItems.stream().mapToInt(CheckoutAvailableItem::subtotal).sum();
        int shippingFee = (availableItems.isEmpty() || itemTotal >= FREE_SHIPPING_THRESHOLD) ? 0 : SHIPPING_FEE;

        return new CheckoutResponse(availableItems, unavailableItems, itemTotal, shippingFee, itemTotal + shippingFee);
    }
}
