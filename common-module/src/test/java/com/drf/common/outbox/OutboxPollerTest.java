package com.drf.common.outbox;

import com.drf.common.infrastructure.kafka.KafkaProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class OutboxPollerTest {

    @Mock
    private OutboxService outboxService;

    @Mock
    private KafkaProducer kafkaProducer;

    @InjectMocks
    private OutboxPoller outboxPoller;

    @Test
    @DisplayName("PENDING 이벤트가 없으면 카프카 전송을 시도하지 않는다")
    void poll_noEvents() {
        // given
        given(outboxService.fetchAndMarkProcessing(anyInt())).willReturn(List.of());

        // when
        outboxPoller.poll();

        // then
        then(kafkaProducer).should(never()).sendSync(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("카프카 전송 성공 시 markPublished를 호출한다")
    void poll_success() {
        // given
        OutboxEvent event = OutboxEvent.create(1L, "ORDER", "OrderCreated", "order-events", "{}");
        given(outboxService.fetchAndMarkProcessing(anyInt())).willReturn(List.of(event));
        given(kafkaProducer.sendSync("order-events", "1", "{}")).willReturn(true);

        // when
        outboxPoller.poll();

        // then
        then(outboxService).should().markPublished(event);
        then(outboxService).should(never()).markFailedOrRetry(any(), anyInt());
    }

    @Test
    @DisplayName("카프카 전송 실패 시 markFailedOrRetry를 호출한다")
    void poll_failure() {
        // given
        OutboxEvent event = OutboxEvent.create(1L, "ORDER", "OrderCreated", "order-events", "{}");
        given(outboxService.fetchAndMarkProcessing(anyInt())).willReturn(List.of(event));
        given(kafkaProducer.sendSync("order-events", "1", "{}")).willReturn(false);

        // when
        outboxPoller.poll();

        // then
        then(outboxService).should().markFailedOrRetry(event, 3);
        then(outboxService).should(never()).markPublished(any());
    }
}
