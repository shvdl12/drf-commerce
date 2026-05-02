package com.drf.order.facade;

import com.drf.common.model.AuthInfo;
import com.drf.order.entity.OrderStatus;
import com.drf.order.model.dto.AmountResult;
import com.drf.order.model.request.OrderCreateRequest;
import com.drf.order.model.response.OrderCreateResponse;
import com.drf.order.saga.OrderCreationSaga;
import com.drf.order.saga.OrderSagaContext;
import com.drf.order.saga.SagaDefinition;
import com.drf.order.saga.SagaExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

    private static final long MEMBER_ID = 1L;
    private static final long ORDER_ID = 1L;
    private static final long ADDRESS_ID = 100L;
    private static final int FINAL_AMOUNT = 21_000;
    private static final String IDEMPOTENCY_KEY = "test-idem-key";

    @Mock
    private SagaExecutor sagaExecutor;
    @Mock
    private OrderCreationSaga orderCreationSaga;

    @InjectMocks
    private OrderFacade orderFacade;

    private AmountResult amounts;

    @BeforeEach
    void setUp() {
        amounts = AmountResult.builder()
                .totalAmount(23_000).productDiscountAmount(2_000)
                .productCouponDiscountAmount(0).orderCouponDiscountAmount(0)
                .deliveryFee(0).finalAmount(FINAL_AMOUNT).build();

        given(orderCreationSaga.definition()).willReturn(SagaDefinition.<OrderSagaContext>builder()
                .name("name")
                .step("noop").invoke(c -> {
                })
                .build());

        // SagaExecutor.execute 호출 시 ctx에 결과 채워주는 stub
        doAnswer(invocation -> {
            OrderSagaContext ctx = invocation.getArgument(1);
            ctx.setOrderId(ORDER_ID);
            ctx.setOrderNo("ORD-TEST-001");
            ctx.setOrderStatus(OrderStatus.PAID);
            ctx.setAmounts(amounts);
            return null;
        }).when(sagaExecutor).execute(any(), any(OrderSagaContext.class));
    }

    @Test
    @DisplayName("createOrder 호출 시 SagaExecutor에 위임하고 결과를 매핑함")
    void createOrder_delegatesToSagaExecutor_andMapsResponse() {
        OrderCreateRequest request = new OrderCreateRequest(
                List.of(10L), ADDRESS_ID, "CARD", FINAL_AMOUNT);

        OrderCreateResponse response = orderFacade.createOrder(new AuthInfo(MEMBER_ID), IDEMPOTENCY_KEY, request);

        then(sagaExecutor).should().execute(any(), any(OrderSagaContext.class));
        assertThat(response.orderId()).isEqualTo(ORDER_ID);
        assertThat(response.orderNo()).isEqualTo("ORD-TEST-001");
        assertThat(response.status()).isEqualTo(OrderStatus.PAID.name());
        assertThat(response.finalAmount()).isEqualTo(FINAL_AMOUNT);
    }
}
