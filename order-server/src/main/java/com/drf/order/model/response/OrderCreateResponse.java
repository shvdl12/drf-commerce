package com.drf.order.model.response;

import lombok.Builder;

@Builder
public record OrderCreateResponse(long orderId, String orderNo, String status, long finalAmount) {
}
