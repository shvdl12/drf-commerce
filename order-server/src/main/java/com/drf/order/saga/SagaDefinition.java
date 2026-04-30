package com.drf.order.saga;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public record SagaDefinition<C>(List<SagaStep<C>> steps) {

    public static <C> Builder<C> builder() {
        return new Builder<>();
    }

    public static class Builder<C> {
        private final List<SagaStep<C>> steps = new ArrayList<>();

        public StepBuilder<C> step(String name) {
            return new StepBuilder<>(this, name);
        }

        public SagaDefinition<C> build() {
            return new SagaDefinition<>(Collections.unmodifiableList(steps));
        }

        void addStep(SagaStep<C> step) {
            steps.add(step);
        }
    }

    @RequiredArgsConstructor
    public static class StepBuilder<C> {
        private final Builder<C> parent;
        private final String name;
        private Consumer<C> action;
        private Consumer<C> compensation;

        public StepBuilder<C> invokeLocal(Consumer<C> action) {
            this.action = action;
            return this;
        }

        public StepBuilder<C> withCompensation(Consumer<C> compensation) {
            this.compensation = compensation;
            return this;
        }

        public StepBuilder<C> step(String nextName) {
            commit();
            return new StepBuilder<>(parent, nextName);
        }

        public SagaDefinition<C> build() {
            commit();
            return parent.build();
        }

        private void commit() {
            if (action == null) {
                throw new IllegalStateException("Saga step '" + name + "' has no action");
            }
            parent.addStep(new SagaStep<>(name, action, compensation));
        }
    }
}
