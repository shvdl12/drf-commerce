package com.drf.product.event.consumer;

import com.drf.common.util.JsonConverter;
import com.drf.product.event.payload.PaymentCompletedPayload;
import com.drf.product.event.payload.RefundCompletedPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private OrderEventProcessor orderEventProcessor;
    @Mock
    private JsonConverter jsonConverter;
    @InjectMocks
    private OrderEventConsumer orderEventConsumer;

    @Test
    @DisplayName("PAYMENT_COMPLETED 이벤트 수신 시 processPaymentCompleted를 호출한다")
    void paymentCompleted_callsProcessPaymentCompleted() throws Exception {
        // given
        String message = """
                {
                  "eventId": 123456789,
                  "eventType": "PAYMENT_COMPLETED",
                  "payload": { "productId": 1, "quantity": 10 }
                }
                """;
        org.mockito.BDDMockito.given(jsonConverter.toJsonNode(message))
                .willReturn(objectMapper.readTree(message));
        org.mockito.BDDMockito.given(jsonConverter.treeToValue(any(), eq(PaymentCompletedPayload.class)))
                .willReturn(new PaymentCompletedPayload(1L, 10));

        // when
        orderEventConsumer.consume(message);

        // then
        then(orderEventProcessor).should().processPaymentCompleted(123456789L, 1L, 10);
    }

    @Test
    @DisplayName("REFUND_COMPLETED 이벤트 수신 시 processRefundCompleted를 호출한다")
    void refundCompleted_callsProcessRefundCompleted() throws Exception {
        // given
        String message = """
                {
                  "eventId": 987654321,
                  "eventType": "REFUND_COMPLETED",
                  "payload": { "productId": 1, "quantity": 10 }
                }
                """;
        org.mockito.BDDMockito.given(jsonConverter.toJsonNode(message))
                .willReturn(objectMapper.readTree(message));
        org.mockito.BDDMockito.given(jsonConverter.treeToValue(any(), eq(RefundCompletedPayload.class)))
                .willReturn(new RefundCompletedPayload(1L, 10));

        // when
        orderEventConsumer.consume(message);

        // then
        then(orderEventProcessor).should().processRefundCompleted(987654321L, 1L, 10);
    }

    @Test
    @DisplayName("중복 이벤트 수신 시 예외 없이 skip한다")
    void duplicateEvent_noException() throws Exception {
        // given
        String message = """
                {
                  "eventId": 111111111,
                  "eventType": "PAYMENT_COMPLETED",
                  "payload": { "productId": 1, "quantity": 10 }
                }
                """;
        org.mockito.BDDMockito.given(jsonConverter.toJsonNode(message))
                .willReturn(objectMapper.readTree(message));
        org.mockito.BDDMockito.given(jsonConverter.treeToValue(any(), eq(PaymentCompletedPayload.class)))
                .willReturn(new PaymentCompletedPayload(1L, 10));
        org.mockito.BDDMockito.willThrow(new DataIntegrityViolationException("duplicate"))
                .given(orderEventProcessor).processPaymentCompleted(anyLong(), anyLong(), anyInt());

        // when & then
        assertThatNoException().isThrownBy(() -> orderEventConsumer.consume(message));
    }

    @Test
    @DisplayName("알 수 없는 이벤트 타입은 예외 없이 무시한다")
    void unknownEventType_noException() throws Exception {
        // given
        String message = """
                {
                  "eventId": 999999999,
                  "eventType": "UNKNOWN_EVENT",
                  "payload": { "productId": 1, "quantity": 10 }
                }
                """;
        org.mockito.BDDMockito.given(jsonConverter.toJsonNode(message))
                .willReturn(objectMapper.readTree(message));

        // when & then
        assertThatNoException().isThrownBy(() -> orderEventConsumer.consume(message));
        then(orderEventProcessor).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("잘못된 JSON 메시지는 예외가 발생한다 - RetryableTopic을 통해 DLT로 이동")
    void invalidJson_throwsException() {
        // given
        String message = "invalid-json";
        org.mockito.BDDMockito.given(jsonConverter.toJsonNode(message))
                .willThrow(new RuntimeException("JSON parse error"));

        // when & then
        assertThatThrownBy(() -> orderEventConsumer.consume(message))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("JSON parse error");
        then(orderEventProcessor).shouldHaveNoInteractions();
    }
}
