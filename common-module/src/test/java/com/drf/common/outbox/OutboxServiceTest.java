package com.drf.common.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private OutboxService outboxService;

    @Test
    @DisplayName("PENDING 이벤트를 조회하고 PROCESSING으로 일괄 업데이트한다")
    void fetchAndMarkProcessing_success() {
        // given
        OutboxEvent event1 = OutboxEvent.create(1L, "ORDER", "OrderCreated", "order-events", "{}");
        OutboxEvent event2 = OutboxEvent.create(2L, "ORDER", "OrderCreated", "order-events", "{}");
        given(outboxEventRepository.findPendingWithLock(100)).willReturn(List.of(event1, event2));

        // when
        List<OutboxEvent> result = outboxService.fetchAndMarkProcessing(100);

        // then
        assertThat(result).hasSize(2);
        then(outboxEventRepository).should().bulkUpdateStatus(List.of(1L, 2L), OutboxStatus.PROCESSING);
    }

    @Test
    @DisplayName("PENDING 이벤트가 없으면 bulkUpdateStatus를 호출하지 않는다")
    void fetchAndMarkProcessing_empty() {
        // given
        given(outboxEventRepository.findPendingWithLock(100)).willReturn(List.of());

        // when
        List<OutboxEvent> result = outboxService.fetchAndMarkProcessing(100);

        // then
        assertThat(result).isEmpty();
        then(outboxEventRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("카프카 발행 성공 시 PUBLISHED로 업데이트한다")
    void markPublished() {
        // given
        OutboxEvent event = OutboxEvent.create(1L, "ORDER", "OrderCreated", "order-events", "{}");

        // when
        outboxService.markPublished(event);

        // then
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isNotNull();
        then(outboxEventRepository).should().save(event);
    }

    @Test
    @DisplayName("재시도 횟수가 한도 미만이면 PENDING으로 복귀한다")
    void markFailedOrRetry_belowMaxRetry() {
        // given
        OutboxEvent event = OutboxEvent.create(1L, "ORDER", "OrderCreated", "order-events", "{}");

        // when
        outboxService.markFailedOrRetry(event, 3);

        // then
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getRetryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("재시도 횟수가 한도에 도달하면 FAILED로 업데이트한다")
    void markFailedOrRetry_reachedMaxRetry() {
        // given
        OutboxEvent event = OutboxEvent.create(1L, "ORDER", "OrderCreated", "order-events", "{}");
        outboxService.markFailedOrRetry(event, 3); // retryCount = 1
        outboxService.markFailedOrRetry(event, 3); // retryCount = 2

        // when
        outboxService.markFailedOrRetry(event, 3); // retryCount = 3 → FAILED

        // then
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(3);
    }
}
