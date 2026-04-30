package com.drf.order.saga;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SagaExecutorTest {

    private final SagaExecutor executor = new SagaExecutor();

    @Test
    @DisplayName("모든 step 성공 시 정방향 순서로 실행됨")
    void allStepsSucceed_executedInOrder() {
        List<String> calls = new ArrayList<>();
        SagaDefinition<Void> def = SagaDefinition.<Void>builder()
                .step("s1").invokeLocal(c -> calls.add("s1"))
                .step("s2").invokeLocal(c -> calls.add("s2"))
                .step("s3").invokeLocal(c -> calls.add("s3"))
                .build();

        executor.execute(def, null);

        assertThat(calls).containsExactly("s1", "s2", "s3");
    }

    @Test
    @DisplayName("n번째 step 실패 시 이전 step의 보상이 역순으로 실행됨")
    void stepFails_compensatesInReverseOrder() {
        List<String> calls = new ArrayList<>();
        SagaDefinition<Void> def = SagaDefinition.<Void>builder()
                .step("s1").invokeLocal(c -> calls.add("s1")).withCompensation(c -> calls.add("s1-comp"))
                .step("s2").invokeLocal(c -> calls.add("s2")).withCompensation(c -> calls.add("s2-comp"))
                .step("s3").invokeLocal(c -> {
                    throw new RuntimeException("fail");
                })
                .build();

        assertThatThrownBy(() -> executor.execute(def, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("fail");

        assertThat(calls).containsExactly("s1", "s2", "s2-comp", "s1-comp");
    }

    @Test
    @DisplayName("보상 중 예외가 발생해도 나머지 보상은 계속 실행됨")
    void compensationFails_continuesRemainingCompensations() {
        List<String> calls = new ArrayList<>();
        SagaDefinition<Void> def = SagaDefinition.<Void>builder()
                .step("s1").invokeLocal(c -> calls.add("s1")).withCompensation(c -> calls.add("s1-comp"))
                .step("s2").invokeLocal(c -> calls.add("s2")).withCompensation(c -> {
                    throw new RuntimeException("compensation fail");
                })
                .step("s3").invokeLocal(c -> {
                    throw new RuntimeException("original fail");
                })
                .build();

        assertThatThrownBy(() -> executor.execute(def, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("original fail");

        assertThat(calls).containsExactly("s1", "s2", "s1-comp");
    }

    @Test
    @DisplayName("보상 없는 step은 실패 시 보상 호출 없이 넘어감")
    void stepWithNoCompensation_skipsCompensation() {
        List<String> calls = new ArrayList<>();
        SagaDefinition<Void> def = SagaDefinition.<Void>builder()
                .step("s1").invokeLocal(c -> calls.add("s1"))
                .step("s2").invokeLocal(c -> {
                    throw new RuntimeException("fail at s2");
                })
                .build();

        assertThatThrownBy(() -> executor.execute(def, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("fail at s2");

        assertThat(calls).containsExactly("s1");
    }

    @Test
    @DisplayName("원 예외가 그대로 rethrow 됨")
    void rethrowsOriginalException() {
        RuntimeException original = new RuntimeException("original");
        SagaDefinition<Void> def = SagaDefinition.<Void>builder()
                .step("s1").invokeLocal(c -> {
                    throw original;
                })
                .build();

        assertThatThrownBy(() -> executor.execute(def, null))
                .isSameAs(original);
    }

    @Test
    void should_throw_exception_when_action_is_null() {
        // given
        var builder = SagaDefinition.<String>builder();

        // when & then
        assertThatThrownBy(() ->
                builder.step("Null action")
                        .invokeLocal(null)
                        .build()
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Saga step 'Null action' has no action");
    }
}
