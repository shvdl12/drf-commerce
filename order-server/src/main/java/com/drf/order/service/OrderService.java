package com.drf.order.service;

import com.drf.common.exception.BusinessException;
import com.drf.order.client.dto.response.DeliveryAddressResponse;
import com.drf.order.common.exception.ErrorCode;
import com.drf.order.entity.*;
import com.drf.order.event.OrderPaidApplicationEvent;
import com.drf.order.model.dto.AmountResult;
import com.drf.order.model.dto.OrderItemData;
import com.drf.order.repository.OrderEventRepository;
import com.drf.order.repository.OrderItemRepository;
import com.drf.order.repository.OrderRepository;
import com.github.f4b6a3.tsid.TsidCreator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderEventRepository orderEventRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Order createOrder(long memberId, List<OrderItemData> items, DeliveryAddressResponse address,
                             Long memberCouponId, AmountResult amountResult) {

        Order order = orderRepository.save(Order.builder()
                .orderNo(TsidCreator.getTsid().toString())
                .memberId(memberId)
                .memberCouponId(memberCouponId)
                .status(OrderStatus.PENDING)
                .totalAmount(amountResult.totalAmount())
                .deliveryFee(amountResult.deliveryFee())
                .productDiscountAmount(amountResult.productDiscountAmount())
                .couponDiscountAmount(amountResult.productCouponDiscountAmount().add(amountResult.orderCouponDiscountAmount()))
                .finalAmount(amountResult.finalAmount())
                .receiverName(address.receiverName())
                .receiverPhone(address.phone())
                .zipCode(address.zipCode())
                .address(address.address())
                .addressDetail(address.addressDetail())
                .build());

        for (OrderItemData item : items) {
            orderItemRepository.save(OrderItem.builder()
                    .orderId(order.getId())
                    .productId(item.productId())
                    .productName(item.productName())
                    .unitPrice(item.unitPrice())
                    .discountedPrice(item.discountedPrice())
                    .quantity(item.quantity())
                    .productCouponDiscountAmount(item.productCouponDiscountAmount())
                    .orderCouponDiscountAmount(item.orderCouponDiscountAmount())
                    .finalAmount(item.finalAmount())
                    .memberCouponId(item.memberCouponId())
                    .status(OrderItemStatus.PENDING)
                    .build());
        }

        return order;
    }

    @Transactional
    public void completePayment(long orderId, long memberId, List<Long> cartItemIds, List<Long> usedMemberCouponIds) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        order.updateStatus(OrderStatus.PAID);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        items.forEach(item -> item.updateStatus(OrderItemStatus.ORDERED));

        orderEventRepository.save(OrderEvent.builder()
                .orderId(orderId)
                .eventType(OrderEventType.ORDER_CREATED)
                .build());

        eventPublisher.publishEvent(new OrderPaidApplicationEvent(orderId, memberId, cartItemIds, usedMemberCouponIds));
    }

    @Transactional
    public void failOrder(long orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.updateStatus(OrderStatus.PAYMENT_FAILED);
            orderItemRepository.findByOrderId(orderId)
                    .forEach(item -> item.updateStatus(OrderItemStatus.PAYMENT_FAILED));
        });
    }
}
