package com.drf.member.event.signup;

import com.drf.member.common.util.JsonConverter;
import com.drf.member.infrastructure.kafka.KafkaProducer;
import com.drf.member.infrastructure.kafka.KafkaTopic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;


@Slf4j
@Component
@RequiredArgsConstructor
public class MemberSignUpEventHandler {
    private final KafkaProducer kafkaProducer;
    private final JsonConverter jsonConverter;


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MemberSignUpEvent event) {
        String key = String.valueOf(event.getPayload().id());
        String payload = jsonConverter.toJson(event);

        kafkaProducer.sendMessage(KafkaTopic.MEMBER.getName(), key, payload, () ->
                log.error("Failed to publish member sign-up event, memberId: {}", event.getPayload().id()));
    }
}
