package com.drf.member.Infrastructure.kafka;

import com.drf.member.infrastructure.kafka.KafkaProducer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private KafkaProducer kafkaProducer;

    @Test
    @DisplayName("발송 성공 시 onError를 호출하지 않는다")
    void sendMessage_success_onErrorNotCalled() {
        // given
        SendResult<String, String> sendResult = mock(SendResult.class);
        TopicPartition partition = new TopicPartition("topic", 0);
        RecordMetadata metadata = new RecordMetadata(partition, 0L, 0, 0L, 0, 0);
        given(sendResult.getRecordMetadata()).willReturn(metadata);

        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(sendResult);
        given(kafkaTemplate.send(anyString(), anyString(), anyString())).willReturn(future);

        Runnable onError = mock(Runnable.class);

        // when
        kafkaProducer.sendMessage("topic", "key", "payload", onError);

        // then
        then(onError).should(never()).run();
    }

    @Test
    @DisplayName("비동기 발송 실패 시 onError를 호출한다")
    void sendMessage_asyncFailed_onErrorCalled() {
        // given
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.failedFuture(
                new RuntimeException()
        );
        given(kafkaTemplate.send(anyString(), anyString(), anyString())).willReturn(future);

        Runnable onError = mock(Runnable.class);

        // when
        kafkaProducer.sendMessage("topic", "key", "payload", onError);

        // then
        then(onError).should(times(1)).run();
    }

    @Test
    @DisplayName("동기 예외 발생 시 onError를 호출한다")
    void sendMessage_syncException_onErrorCalled() {
        // given
        given(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .willThrow(new RuntimeException());

        Runnable onError = mock(Runnable.class);

        // when
        kafkaProducer.sendMessage("topic", "key", "payload", onError);

        // then
        then(onError).should(times(1)).run();
    }
}