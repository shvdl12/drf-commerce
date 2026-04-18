package com.drf.order.client.dto.response;

public record ProductResponse(long id, String name, int price, int stock, String status) {
}
