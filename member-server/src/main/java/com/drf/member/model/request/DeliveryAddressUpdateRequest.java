package com.drf.member.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeliveryAddressUpdateRequest(
        @NotBlank @Size(max = 50)
        String name,

        @NotBlank @Size(max = 20)
        String phone,

        @NotBlank @Size(max = 255)
        String address,

        @NotBlank @Size(max = 100)
        String addressDetail,

        @NotBlank @Size(max = 10)
        String zipCode,

        boolean isDefault
) {
}
