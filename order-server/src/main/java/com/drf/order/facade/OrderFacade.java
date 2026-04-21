package com.drf.order.facade;

import com.drf.common.exception.BusinessException;
import com.drf.common.model.AuthInfo;
import com.drf.order.client.CouponClient;
import com.drf.order.client.MemberClient;
import com.drf.order.client.PaymentClient;
import com.drf.order.client.ProductClient;
import com.drf.order.client.dto.request.*;
import com.drf.order.client.dto.request.CouponBatchReserveRequest.CouponBatchReserveItem;
import com.drf.order.client.dto.request.StockBatchReleaseRequest.StockBatchReleaseItem;
import com.drf.order.client.dto.request.StockBatchReserveRequest.StockBatchReserveItem;
import com.drf.order.client.dto.response.*;
import com.drf.order.common.exception.ErrorCode;
import com.drf.order.entity.Cart;
import com.drf.order.entity.CartItem;
import com.drf.order.entity.Order;
import com.drf.order.model.dto.AmountResult;
import com.drf.order.model.dto.CartItemsResult;
import com.drf.order.model.dto.OrderLineItem;
import com.drf.order.model.request.OrderCreateRequest;
import com.drf.order.model.response.OrderCreateResponse;
import com.drf.order.service.CartService;
import com.drf.order.service.OrderService;
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
public class OrderFacade {

    private static final int FREE_SHIPPING_THRESHOLD = 50_000;
    private static final int SHIPPING_FEE = 3_000;
    private static final String STOCK_RESERVE_KEY_SUFFIX = ":STOCK_RESERVE";
    private static final String STOCK_RELEASE_KEY_SUFFIX = ":STOCK_RELEASE";

    private final CartService cartService;
    private final ProductClient productClient;
    private final CouponClient couponClient;
    private final MemberClient memberClient;
    private final PaymentClient paymentClient;
    private final OrderService orderService;


    public OrderCreateResponse createOrder(AuthInfo authInfo, String idempotencyKey, OrderCreateRequest request) {
        long memberId = authInfo.id();

        // 1. 장바구니 상품 조회 + 소유권 검증
        CartItemsResult cartResult = cartService.getValidatedCartItems(memberId, request.cartItemIds());
        Cart cart = cartResult.cart();

        // 2. OrderLineItem 빌드 (상품 조회)
        List<OrderLineItem> lineItems = buildLineItems(cartResult.items());

        // 3. 상품쿠폰 할인 계산
        applyProductCouponDiscounts(memberId, lineItems);

        // 4. 카트쿠폰 할인 적용
        applyCartCouponDiscount(memberId, cart, lineItems);

        // 5. 금액 계산 + 기대금액 검증
        AmountResult amounts = calculateAmounts(lineItems);
        if (amounts.finalAmount() != request.expectedAmount()) {
            throw new BusinessException(ErrorCode.ORDER_AMOUNT_MISMATCH);
        }

        // 6. 배송지 조회
        DeliveryAddressResponse address;
        try {
            address = memberClient.getDeliveryAddress(memberId, request.shippingAddressId()).getData();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SHIPPING_ADDRESS_NOT_FOUND);
        }

        // 7. 주문 생성 (PENDING)
        Order order = orderService.createOrder(memberId,
                lineItems.stream().map(OrderLineItem::toOrderItemData).toList(),
                address, cart.getCouponId(),
                amounts.deliveryFee(), amounts.totalAmount(), amounts.productDiscountAmount(),
                amounts.couponDiscountAmount(), amounts.finalAmount());

        // 8. 재고 선점
        try {
            productClient.reserveStock(idempotencyKey + STOCK_RESERVE_KEY_SUFFIX,
                    new StockBatchReserveRequest(lineItems.stream()
                            .map(item -> new StockBatchReserveItem(item.getProductId(), item.getQuantity()))
                            .toList()));
        } catch (Exception e) {
            log.error("Stock batch reserve failed", e);
            orderService.failOrder(order.getId());
            throw new BusinessException(ErrorCode.ORDER_STOCK_INSUFFICIENT);
        }

        // 9. 쿠폰 선점
        List<Long> allMemberCouponIds = buildCouponIds(cart.getCouponId(), lineItems);
        if (!allMemberCouponIds.isEmpty()) {
            try {
                couponClient.reserveCoupon(new CouponBatchReserveRequest(allMemberCouponIds.stream()
                        .map(id -> new CouponBatchReserveItem(id, memberId))
                        .toList()));
            } catch (Exception e) {
                log.error("Coupon batch reserve failed", e);
                releaseStocks(lineItems, idempotencyKey);
                orderService.failOrder(order.getId());
                throw new BusinessException(ErrorCode.ORDER_COUPON_UNAVAILABLE);
            }
        }

        // 10. 결제
        try {
            paymentClient.pay(new PaymentRequest(order.getId(), amounts.finalAmount(), request.paymentMethodId()));
        } catch (Exception e) {
            log.error("Payment failed for orderId={}", order.getId(), e);
            releaseCoupons(allMemberCouponIds, memberId);
            releaseStocks(lineItems, idempotencyKey);
            orderService.failOrder(order.getId());
            throw new BusinessException(ErrorCode.ORDER_PAYMENT_FAILED);
        }

        // 11. 결제 완료 처리 (트랜잭션 커밋 후 Kafka ORDER_PAID 이벤트 자동 발행)
        orderService.completePayment(order.getId(), memberId,
                lineItems.stream().map(OrderLineItem::getCartItemId).toList(),
                allMemberCouponIds);

        return new OrderCreateResponse(order.getId(), order.getOrderNo(), order.getStatus().name(), amounts.finalAmount());
    }

    private List<OrderLineItem> buildLineItems(List<CartItem> cartItems) {
        List<Long> productIds = cartItems.stream().map(CartItem::getProductId).toList();

        Map<Long, InternalProductResponse> productMap = new HashMap<>();
        ProductBatchRequest productBatchRequest = new ProductBatchRequest(productIds);
        for (InternalProductResponse p : productClient.getProductsBatch(productBatchRequest).getData()) {
            productMap.put(p.id(), p);
        }

        List<OrderLineItem> lineItems = new ArrayList<>();
        for (CartItem item : cartItems) {
            lineItems.add(OrderLineItem.of(item, productMap.get(item.getProductId())));
        }
        return lineItems;
    }

    private void applyProductCouponDiscounts(long memberId, List<OrderLineItem> lineItems) {
        for (OrderLineItem lineItem : lineItems) {
            if (lineItem.getMemberCouponId() == null) continue;
            InternalProductCouponRequest req = new InternalProductCouponRequest(
                    memberId, lineItem.getCartItemId(), lineItem.getProductId(),
                    lineItem.getDiscountedPrice(), lineItem.getQuantity(),
                    lineItem.getCategoryPath(), List.of());
            ProductCouponCalculateResponse r = couponClient
                    .calculateProductCoupon(lineItem.getMemberCouponId(), req).getData();
            lineItem.applyProductCouponDiscount(r.applicable() ? r.discountAmount() : 0);
        }
    }

    private void applyCartCouponDiscount(long memberId, Cart cart, List<OrderLineItem> lineItems) {
        if (cart.getCouponId() == null) return;
        List<InternalCartCouponItemRequest> couponItems = lineItems.stream()
                .map(item -> new InternalCartCouponItemRequest(
                        item.getCartItemId(), item.getProductId(), item.getDiscountedPrice(),
                        item.getQuantity(), item.getCategoryPath()))
                .toList();
        InternalCartCouponCalculateResponse r = couponClient
                .calculateCartCoupon(cart.getCouponId(), new InternalCartCouponRequest(memberId, couponItems))
                .getData();
        if (!r.applicable()) return;
        Map<Long, Integer> discountByItem = new HashMap<>();
        for (InternalCouponItemResult result : r.items()) {
            discountByItem.put(result.cartItemId(), result.discountAmount());
        }
        for (OrderLineItem item : lineItems) {
            item.applyOrderCouponDiscount(discountByItem.getOrDefault(item.getCartItemId(), 0));
        }
    }

    private AmountResult calculateAmounts(List<OrderLineItem> lineItems) {
        int totalAmount = lineItems.stream().mapToInt(OrderLineItem::subtotal).sum();
        int productDiscountAmount = lineItems.stream()
                .mapToInt(item -> (item.getUnitPrice() - item.getDiscountedPrice()) * item.getQuantity()).sum();
        int couponDiscountAmount = lineItems.stream().mapToInt(OrderLineItem::totalCouponDiscount).sum();
        int netAmount = totalAmount - couponDiscountAmount;
        int deliveryFee = netAmount >= FREE_SHIPPING_THRESHOLD ? 0 : SHIPPING_FEE;
        return new AmountResult(totalAmount, productDiscountAmount, couponDiscountAmount, deliveryFee, netAmount + deliveryFee);
    }

    private List<Long> buildCouponIds(Long cartCouponId, List<OrderLineItem> lineItems) {
        List<Long> ids = new ArrayList<>();
        if (cartCouponId != null) ids.add(cartCouponId);
        for (OrderLineItem item : lineItems) {
            if (item.getMemberCouponId() != null && !ids.contains(item.getMemberCouponId())) {
                ids.add(item.getMemberCouponId());
            }
        }
        return ids;
    }

    private void releaseStocks(List<OrderLineItem> lineItems, String idempotencyKey) {
        try {
            productClient.releaseStock(idempotencyKey + STOCK_RELEASE_KEY_SUFFIX,
                    new StockBatchReleaseRequest(lineItems.stream()
                            .map(item -> new StockBatchReleaseItem(item.getProductId(), item.getQuantity()))
                            .toList()));
        } catch (Exception e) {
            log.error("Stock batch release failed", e);
        }
    }

    private void releaseCoupons(List<Long> memberCouponIds, long memberId) {
        if (memberCouponIds.isEmpty()) return;
        try {
            couponClient.releaseCoupon(new CouponBatchReserveRequest(memberCouponIds.stream()
                    .map(id -> new CouponBatchReserveItem(id, memberId))
                    .toList()));
        } catch (Exception e) {
            log.error("Coupon batch release failed", e);
        }
    }
}
