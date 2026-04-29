package com.drf.order.service;

import com.drf.order.client.PaymentClient;
import com.drf.order.client.dto.request.PaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderPaymentService {

    private final PaymentClient paymentClient;

    public void pay(Long orderId, int finalAmount, String paymentMethodId) {
        paymentClient.pay(new PaymentRequest(orderId, finalAmount, paymentMethodId));
    }
}