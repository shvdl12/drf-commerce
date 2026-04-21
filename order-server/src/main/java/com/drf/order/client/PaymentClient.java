package com.drf.order.client;

import com.drf.order.client.dto.request.PaymentRequest;
import com.drf.order.client.dto.response.PaymentResponse;

public interface PaymentClient {

    PaymentResponse pay(PaymentRequest request);
}
