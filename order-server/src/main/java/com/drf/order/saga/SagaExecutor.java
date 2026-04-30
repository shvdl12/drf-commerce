package com.drf.order.saga;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class SagaExecutor {

    public <C> void execute(SagaDefinition<C> definition, C context) {
        List<SagaStep<C>> steps = definition.steps();
        int executed = 0;
        try {
            for (SagaStep<C> step : steps) {
                step.action().accept(context);
                executed++;
            }
        } catch (RuntimeException ex) {
            compensate(steps, executed, context);
            throw ex;
        }
    }

    private <C> void compensate(List<SagaStep<C>> steps, int executed, C context) {
        for (int i = executed - 1; i >= 0; i--) {
            SagaStep<C> step = steps.get(i);

            if (step.compensation() == null) {
                continue;
            }

            try {
                step.compensation().accept(context);
            } catch (RuntimeException ce) {
                log.error("Saga compensation '{}' failed", step.name(), ce);
            }
        }
    }
}
