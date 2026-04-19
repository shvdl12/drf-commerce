package com.drf.coupon.event.consumer;

import com.drf.common.event.EventEnvelope;
import com.drf.common.util.JsonConverter;
import com.drf.coupon.event.payload.PaymentCompletedPayload;
import com.drf.coupon.event.payload.RefundCompletedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final OrderEventProcessor orderEventProcessor;
    private final JsonConverter jsonConverter;

    @RetryableTopic(
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            exclude = {IllegalArgumentException.class}
    )
    @KafkaListener(topics = "#{T(com.drf.common.event.EventTopic).ORDER.getName()}")
    public void consume(String message) {
        long eventId = 0L;
        try {
            EventEnvelope envelope = jsonConverter.fromJson(message, EventEnvelope.class);
            eventId = envelope.eventId();

            switch (envelope.eventType()) {
                case "PAYMENT_COMPLETED" -> {
                    PaymentCompletedPayload p = jsonConverter.treeToValue(envelope.payload(), PaymentCompletedPayload.class);
                    orderEventProcessor.processPaymentCompleted(eventId, p.memberCouponId());
                }
                case "REFUND_COMPLETED" -> {
                    RefundCompletedPayload p = jsonConverter.treeToValue(envelope.payload(), RefundCompletedPayload.class);
                    orderEventProcessor.processRefundCompleted(eventId, p.memberCouponId());
                }
                default -> log.warn("Unknown order event type: {}", envelope.eventType());
            }
        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate event skipped. eventId={}", eventId);
        }
    }

    @DltHandler
    public void handleDlt(String message,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          @Header(KafkaHeaders.EXCEPTION_MESSAGE) String errorMessage) {
        log.error("Event moved to DLT. topic={}, errorMessage={}, message={}", topic, errorMessage, message);
    }
}
