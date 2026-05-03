package com.drf.order.client.dto.response;

public record ProductResponse(long id, String name, long price, int stock, String status) {
}
