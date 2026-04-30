package com.drf.order.saga;

import java.util.function.Consumer;

public record SagaStep<C>(String name, Consumer<C> action, Consumer<C> compensation) {
}
