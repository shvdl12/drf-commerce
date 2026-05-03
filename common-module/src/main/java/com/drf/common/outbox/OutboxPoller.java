package com.drf.common.outbox;

import com.drf.common.infrastructure.kafka.KafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRY = 3;

    private final OutboxService outboxService;
    private final KafkaProducer kafkaProducer;

    @Scheduled(fixedDelayString = "${outbox.poller.interval-ms:1000}")
    public void poll() {
        List<OutboxEvent> events = outboxService.fetchAndMarkProcessing(BATCH_SIZE);
        if (events.isEmpty()) return;

        for (OutboxEvent event : events) {
            boolean success = kafkaProducer.sendSync(event.getTopic(), String.valueOf(event.getEventId()), event.getPayload());
            if (success) {
                outboxService.markPublished(event);
            } else {
                outboxService.markFailedOrRetry(event, MAX_RETRY);
            }
        }
    }
}
