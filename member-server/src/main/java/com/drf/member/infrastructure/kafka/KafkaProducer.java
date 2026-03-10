package com.drf.member.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendMessage(String topic, String key, String payload, Runnable onError) {
        try {
            kafkaTemplate.send(topic, key, payload)
                    .whenComplete((result, e) -> {
                        if (e == null) {
                            log.info("Sent message=[{}] with offset=[{}]", payload, result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to kafka produce: {}", payload, e);
                            onError.run();
                        }
                    });

        } catch (Exception e) {
            log.error("Failed to kafka produce: {}", payload, e);
            onError.run();
        }
    }
}
