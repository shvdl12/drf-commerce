package com.drf.order.facade;

import com.drf.common.model.AuthInfo;
import com.drf.order.entity.Order;
import com.drf.order.model.request.OrderCreateRequest;
import com.drf.order.model.response.OrderCreateResponse;
import com.drf.order.saga.OrderCreationSaga;
import com.drf.order.saga.OrderSagaContext;
import com.drf.order.saga.SagaExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderFacade {

    private final SagaExecutor sagaExecutor;
    private final OrderCreationSaga orderCreationSaga;

    public OrderCreateResponse createOrder(AuthInfo authInfo, String idempotencyKey, OrderCreateRequest request) {
        OrderSagaContext ctx = new OrderSagaContext(authInfo.id(), idempotencyKey, request);
        sagaExecutor.execute(orderCreationSaga.definition(), ctx);
        Order order = ctx.getOrder();

        return OrderCreateResponse.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .status(order.getStatus().name())
                .finalAmount(ctx.getAmounts().finalAmount())
                .build();
    }
}
