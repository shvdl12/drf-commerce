package com.drf.common.infrastructure.kafka;


import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

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
        SendResult<String, String> sendResult = Mockito.mock(SendResult.class);
        TopicPartition partition = new TopicPartition("topic", 0);
        RecordMetadata metadata = new RecordMetadata(partition, 0L, 0, 0L, 0, 0);
        BDDMockito.given(sendResult.getRecordMetadata()).willReturn(metadata);

        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(sendResult);
        BDDMockito.given(kafkaTemplate.send(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).willReturn(future);

        Runnable onError = Mockito.mock(Runnable.class);

        // when
        kafkaProducer.sendMessage("topic", "key", "payload", onError);

        // then
        BDDMockito.then(onError).should(Mockito.never()).run();
    }

    @Test
    @DisplayName("비동기 발송 실패 시 onError를 호출한다")
    void sendMessage_asyncFailed_onErrorCalled() {
        // given
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.failedFuture(
                new RuntimeException()
        );
        BDDMockito.given(kafkaTemplate.send(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).willReturn(future);

        Runnable onError = Mockito.mock(Runnable.class);

        // when
        kafkaProducer.sendMessage("topic", "key", "payload", onError);

        // then
        BDDMockito.then(onError).should(Mockito.times(1)).run();
    }

    @Test
    @DisplayName("동기 예외 발생 시 onError를 호출한다")
    void sendMessage_syncException_onErrorCalled() {
        // given
        BDDMockito.given(kafkaTemplate.send(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .willThrow(new RuntimeException());

        Runnable onError = Mockito.mock(Runnable.class);

        // when
        kafkaProducer.sendMessage("topic", "key", "payload", onError);

        // then
        BDDMockito.then(onError).should(Mockito.times(1)).run();
    }
}