package com.drf.order.client.dto;

public record ProductResponse(long id, String name, int price, int stock, String status) {
}
