package com.drf.order.client;

import com.drf.order.client.dto.request.PaymentRequest;
import com.drf.order.client.dto.response.PaymentResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StubPaymentClient implements PaymentClient {

    @Override
    public PaymentResponse pay(PaymentRequest request) {
        return new PaymentResponse("STUB-" + UUID.randomUUID());
    }
}
