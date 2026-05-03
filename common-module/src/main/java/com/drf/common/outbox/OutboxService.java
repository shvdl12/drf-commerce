package com.drf.common.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public List<OutboxEvent> fetchAndMarkProcessing(int batchSize) {
        List<OutboxEvent> events = outboxEventRepository.findPendingWithLock(batchSize);
        if (events.isEmpty()) return events;

        List<Long> ids = events.stream().map(OutboxEvent::getEventId).toList();
        outboxEventRepository.bulkUpdateStatus(ids, OutboxStatus.PROCESSING);
        return events;
    }

    @Transactional
    public void markPublished(OutboxEvent event) {
        event.markPublished();
        outboxEventRepository.save(event);
    }

    @Transactional
    public void markFailedOrRetry(OutboxEvent event, int maxRetry) {
        event.incrementRetry();
        if (event.getRetryCount() >= maxRetry) {
            event.markFailed();
            log.error("Outbox event failed after {} retries. eventId={}", maxRetry, event.getEventId());
        } else {
            event.markPending();
        }
        outboxEventRepository.save(event);
    }
}
